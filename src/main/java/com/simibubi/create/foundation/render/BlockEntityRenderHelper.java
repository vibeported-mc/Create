package com.simibubi.create.foundation.render;

import java.util.BitSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class BlockEntityRenderHelper {
	/**
	 * Renders the given list of BlockEntities, skipping those not marked in shouldRenderBEs,
	 * and marking those that error in erroredBEsOut.
	 * <p>
	 * Minecraft 26.2 splits a block entity renderer into an extract phase and a submit phase. Both
	 * run back to back here: a contraption's block entities are not part of the level's own render
	 * pass, so nothing else is going to extract them, and their state is discarded straight after.
	 *
	 * @param blockEntities   The list of BlockEntities to render.
	 * @param shouldRenderBEs A BitSet marking which BlockEntities in the list should be rendered. This will not be modified.
	 * @param erroredBEsOut   A BitSet to mark BlockEntities that error during rendering. This will be modified.
	 */
	public static void renderBlockEntities(List<BlockEntity> blockEntities, BitSet shouldRenderBEs,
		BitSet erroredBEsOut, @javax.annotation.Nullable VirtualRenderWorld renderLevel, Level realLevel, PoseStack ms,
		@javax.annotation.Nullable Matrix4f lightTransform, SubmitNodeCollector queue, CameraRenderState camera,
		Vec3 cameraPosition, float pt) {
		for (int i = shouldRenderBEs.nextSetBit(0); i >= 0 && i < blockEntities.size(); i = shouldRenderBEs.nextSetBit(i + 1)) {
			BlockEntity blockEntity = blockEntities.get(i);
			if (VisualizationManager.supportsVisualization(realLevel) && VisualizationHelper.skipVanillaRender(blockEntity))
				continue;

			BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = getRenderer(blockEntity);
			if (renderer == null) {
				// Don't bother looping over it again if we can't do anything with it.
				erroredBEsOut.set(i);
				continue;
			}

			BlockPos pos = blockEntity.getBlockPos();
			ms.pushPose();
			TransformStack.of(ms)
				.translate(pos);

			try {
				int realLevelLight = LightCoordsUtil.getLightCoords(realLevel, getLightPos(lightTransform, pos));

				int light;
				if (renderLevel != null) {
					renderLevel.setExternalLight(realLevelLight);
					light = LightCoordsUtil.getLightCoords(renderLevel, pos);
				} else {
					light = realLevelLight;
				}

				BlockEntityRenderState state = renderer.createRenderState();
				renderer.extractRenderState(blockEntity, state, pt, cameraPosition, null);
				// Extraction takes the light from the block entity's own level; inside a contraption
				// the position it is drawn at is not where it lives, so the light is overridden.
				state.lightCoords = light;
				renderer.submit(state, ms, queue, camera);

			} catch (Exception e) {
				// Prevent this BE from causing more issues in the future.
				erroredBEsOut.set(i);

				String message = "BlockEntity " + RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType()) + " could not be rendered virtually.";
				if (AllConfigs.client().explainRenderErrors.get()) Create.LOGGER.error(message, e);
				else Create.LOGGER.error(message);
			}

			ms.popPose();
		}

		if (renderLevel != null) {
			renderLevel.resetExternalLight();
		}
	}

	/**
	 * The dispatcher's renderers are keyed by block entity type, so the render state type cannot be
	 * known statically here; a renderer only ever sees the state it created itself.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static BlockEntityRenderer<BlockEntity, BlockEntityRenderState> getRenderer(BlockEntity blockEntity) {
		return (BlockEntityRenderer<BlockEntity, BlockEntityRenderState>) Minecraft.getInstance()
			.getBlockEntityRenderDispatcher()
			.getRenderer(blockEntity);
	}

	private static BlockPos getLightPos(@Nullable Matrix4f lightTransform, BlockPos contraptionPos) {
		if (lightTransform != null) {
			Vector4f lightVec = new Vector4f(contraptionPos.getX() + .5f, contraptionPos.getY() + .5f, contraptionPos.getZ() + .5f, 1);
			lightVec.mul(lightTransform);
			return BlockPos.containing(lightVec.x(), lightVec.y(), lightVec.z());
		} else {
			return contraptionPos;
		}
	}

}
