package com.simibubi.create.foundation.render;

import java.util.ArrayList;
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

/**
 * Renders the block entities of a contraption, which are not part of the level's own render pass.
 * <p>
 * Minecraft 26.2 splits a block entity renderer into an extract phase, which may read the block
 * entity, and a submit phase, which may not. Contraption block entities live in a virtual level and
 * nothing else is going to extract them, so that happens here:
 * {@link #extractBlockEntities} produces the states and {@link Extracted#submit} replays them.
 */
public class BlockEntityRenderHelper {

	/**
	 * A block entity's renderer paired with the state it produced, positioned within the contraption.
	 */
	private record Entry(BlockEntityRenderer<?, BlockEntityRenderState> renderer, BlockEntityRenderState state,
		BlockPos pos) {
	}

	public record Extracted(List<Entry> entries, @Nullable PoseStack model) {
		@SuppressWarnings("unchecked")
		public void submit(PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
			ms.pushPose();

			// The contraption's own transform - how it is turned and where its anchor sits. The structure
			// mesh has it baked in when it is extracted, and so do the actors, but a block entity is drawn
			// by its own renderer off the stack it is handed. Without it put back on here, such a block is
			// drawn facing the way it was built and offset from the contraption's anchor, while everything
			// around it turns.
			if (model != null) {
				ms.last()
					.pose()
					.mul(model.last()
						.pose());
				ms.last()
					.normal()
					.mul(model.last()
						.normal());
			}

			for (Entry entry : entries) {
				ms.pushPose();
				TransformStack.of(ms)
					.translate(entry.pos());
				((BlockEntityRenderer<?, BlockEntityRenderState>) entry.renderer()).submit(entry.state(), ms, queue,
					camera);
				ms.popPose();
			}

			ms.popPose();
		}
	}

	/**
	 * Extracts the given list of BlockEntities, skipping those not marked in shouldRenderBEs,
	 * and marking those that error in erroredBEsOut.
	 *
	 * @param blockEntities   The list of BlockEntities to extract.
	 * @param shouldRenderBEs A BitSet marking which BlockEntities in the list should be rendered. This will not be modified.
	 * @param erroredBEsOut   A BitSet to mark BlockEntities that error during extraction. This will be modified.
	 * @param model           How the thing being drawn is placed and turned, or null if it is neither. Kept
	 *                        for the submit phase, which cannot ask for it again.
	 */
	public static Extracted extractBlockEntities(List<BlockEntity> blockEntities, BitSet shouldRenderBEs,
		BitSet erroredBEsOut, @javax.annotation.Nullable VirtualRenderWorld renderLevel, Level realLevel,
		@javax.annotation.Nullable PoseStack model, @javax.annotation.Nullable Matrix4f lightTransform,
		Vec3 cameraPosition, float pt) {
		List<Entry> entries = new ArrayList<>();

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
				entries.add(new Entry(renderer, state, pos));

			} catch (Exception e) {
				// Prevent this BE from causing more issues in the future.
				erroredBEsOut.set(i);

				String message = "BlockEntity " + RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType()) + " could not be rendered virtually.";
				if (AllConfigs.client().explainRenderErrors.get()) Create.LOGGER.error(message, e);
				else Create.LOGGER.error(message);
			}
		}

		if (renderLevel != null) {
			renderLevel.resetExternalLight();
		}

		return new Extracted(entries, copyOf(model));
	}

	/**
	 * The stack a transform is read from is reused and cleared as soon as extraction is over, so what the
	 * submit phase needs is taken off it here.
	 */
	@Nullable
	private static PoseStack copyOf(@javax.annotation.Nullable PoseStack model) {
		if (model == null)
			return null;

		PoseStack copy = new PoseStack();

		copy.last()
			.pose()
			.mul(model.last()
				.pose());
		copy.last()
			.normal()
			.mul(model.last()
				.normal());

		return copy;
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
