package com.simibubi.create.content.trains.bogey;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BogeyBlockEntityRenderer<T extends AbstractBogeyBlockEntity>
	extends SafeBlockEntityRenderer<T, BogeyBlockEntityRenderer.BogeyRenderState> {

	public static class BogeyRenderState extends SafeRenderState {
		public final List<BogeyRenderer.Part> parts = new ArrayList<>();
		/** A bogey laid along X is turned a quarter turn; that is a pose transform, not geometry. */
		public boolean alongX;
	}

	public BogeyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BogeyRenderState createRenderState() {
		return new BogeyRenderState();
	}

	@Override
	protected void extractSafe(T be, BogeyRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.parts.clear();

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey)) {
			state.skip = true;
			return;
		}

		state.alongX = blockState.getValue(AbstractBogeyBlock.AXIS) == Direction.Axis.X;
		be.getStyle()
			.extract(bogey.getSize(), partialTicks, state.lightCoords, be.getVirtualAngle(partialTicks),
				be.getBogeyData(), false, state.parts);
	}

	@Override
	protected void submitSafe(BogeyRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		if (state.alongX)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		BogeyStyle.submit(state.parts, ms, queue);
		ms.popPose();
	}
}
