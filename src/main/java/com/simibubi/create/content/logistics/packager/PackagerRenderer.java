package com.simibubi.create.content.logistics.packager;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.content.logistics.depot.DepotRenderer.ItemState;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PackagerRenderer
	extends SmartBlockEntityRenderer<PackagerBlockEntity, PackagerRenderer.PackagerRenderState> {

	public static class PackagerRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState hatch;
		public @Nullable SuperByteBufferRenderState tray;
		public @Nullable ItemState box;
		public float trayOffset;
		public Direction facing = Direction.NORTH;
	}

	public PackagerRenderer(Context context) {
		super(context);
	}

	@Override
	public PackagerRenderState createRenderState() {
		return new PackagerRenderState();
	}

	@Override
	protected void extractSafe(PackagerBlockEntity be, PackagerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.hatch = null;
		state.tray = null;
		state.box = null;

		ItemStack renderedBox = be.getRenderedBox();
		state.trayOffset = be.getTrayOffset(partialTicks);
		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(PackagerBlock.FACING)
			.getOpposite();
		state.facing = facing;

		if (!VisualizationManager.supportsVisualization(be.getLevel())) {
			SuperByteBuffer hatch = CachedBuffers.partial(getHatchModel(be), blockState);
			TransformStack.of(hatch.getTransforms())
				.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
					.scale(.49999f))
				.rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing))
				.rotateXCenteredDegrees(AngleHelper.verticalAngle(facing));
			state.hatch = hatch.light(state.lightCoords)
				.extractRenderState();

			SuperByteBuffer tray = CachedBuffers.partial(getTrayModel(blockState), blockState);
			TransformStack.of(tray.getTransforms())
				.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
					.scale(state.trayOffset))
				.rotateYCenteredDegrees(facing.toYRot());
			state.tray = tray.light(state.lightCoords)
				.extractRenderState();
		}

		if (!renderedBox.isEmpty())
			state.box = ItemState.create(itemModelResolver, renderedBox, be.getLevel());
	}

	@Override
	protected void submitSafe(PackagerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		if (state.hatch != null)
			state.hatch.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.tray != null)
			state.tray.submit(ms, RenderTypes.cutoutMovingBlock(), queue);

		if (state.box == null)
			return;

		ms.pushPose();
		var msr = TransformStack.of(ms);
		msr.translate(Vec3.atLowerCornerOf(state.facing.getUnitVec3i())
			.scale(state.trayOffset))
			.translate(.5f, .5f, .5f)
			.rotateYDegrees(state.facing.toYRot())
			.translate(0, 2 / 16f, 0)
			.scale(1.49f, 1.49f, 1.49f);
		state.box.item()
			.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static PartialModel getTrayModel(BlockState blockState) {
		return AllBlocks.PACKAGER.has(blockState) ? AllPartialModels.PACKAGER_TRAY_REGULAR
			: AllPartialModels.PACKAGER_TRAY_DEFRAG;
	}

	public static PartialModel getHatchModel(PackagerBlockEntity be) {
		return isHatchOpen(be) ? AllPartialModels.PACKAGER_HATCH_OPEN : AllPartialModels.PACKAGER_HATCH_CLOSED;
	}

	public static boolean isHatchOpen(PackagerBlockEntity be) {
		return be.animationTicks > (be.animationInward ? 1 : 5)
			&& be.animationTicks < PackagerBlockEntity.CYCLE - (be.animationInward ? 5 : 1);
	}

}
