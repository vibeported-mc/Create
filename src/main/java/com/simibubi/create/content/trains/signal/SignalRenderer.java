package com.simibubi.create.content.trains.signal;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SignalRenderer extends SafeBlockEntityRenderer<SignalBlockEntity, SignalRenderer.SignalRenderState> {

	public static class SignalRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState lamp;
		public @Nullable Level level;
		public @Nullable BlockPos targetPosition;
		public @Nullable BlockPos offset;
		public @Nullable AxisDirection targetDirection;
		public @Nullable BezierTrackPointLocation targetBezier;
		public @Nullable RenderedTrackOverlayType overlayType;
	}

	public SignalRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public SignalRenderState createRenderState() {
		return new SignalRenderState();
	}

	@Override
	protected void extractSafe(SignalBlockEntity be, SignalRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.lamp = null;
		state.overlayType = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		SignalState signalState = be.getState();
		OverlayState overlayState = be.getOverlay();

		float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());
		if (signalState.isRedLight(renderTime))
			state.lamp = CachedBufferer.partial(AllPartialModels.SIGNAL_ON, blockState)
				.light(LightCoordsUtil.FULL_BLOCK)
				.extractRenderState();
		else
			state.lamp = CachedBufferer.partial(AllPartialModels.SIGNAL_OFF, blockState)
				.light(state.lightCoords)
				.extractRenderState();

		BlockPos pos = be.getBlockPos();
		TrackTargetingBehaviour<SignalBoundary> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;
		if (overlayState == OverlayState.SKIP)
			return;

		// The overlay's placement is resolved by the track block against the live PoseStack, so its
		// inputs are carried across and the geometry is built during submission.
		state.level = level;
		state.targetPosition = targetPosition;
		state.offset = targetPosition.subtract(pos);
		state.targetDirection = target.getTargetDirection();
		state.targetBezier = target.getTargetBezier();
		state.overlayType = overlayState == OverlayState.DUAL ? RenderedTrackOverlayType.DUAL_SIGNAL
			: RenderedTrackOverlayType.SIGNAL;
	}

	@Override
	protected void submitSafe(SignalRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.lamp != null)
			state.lamp.submit(ms, RenderTypes.solidMovingBlock(), queue);

		if (state.overlayType == null || state.level == null || state.targetPosition == null)
			return;

		ms.pushPose();
		TransformStack.of(ms)
			.translate(state.offset);
		TrackTargetingBehaviour.submit(state.level, state.targetPosition, state.targetDirection, state.targetBezier,
			ms, queue, state.overlayType, 1);
		ms.popPose();
	}

}
