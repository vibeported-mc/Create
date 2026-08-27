package com.simibubi.create.content.trains.observer;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrackObserverRenderer
	extends SmartBlockEntityRenderer<TrackObserverBlockEntity, TrackObserverRenderer.TrackObserverRenderState> {

	public static class TrackObserverRenderState extends SmartRenderState {
		public boolean renderOverlay;
		public @Nullable Level level;
		public @Nullable BlockPos targetPosition;
		public @Nullable BlockPos offset;
		public @Nullable AxisDirection targetDirection;
		public @Nullable BezierTrackPointLocation targetBezier;
	}

	public TrackObserverRenderer(Context context) {
		super(context);
	}

	@Override
	public TrackObserverRenderState createRenderState() {
		return new TrackObserverRenderState();
	}

	@Override
	protected void extractSafe(TrackObserverBlockEntity be, TrackObserverRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.renderOverlay = false;

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockPos pos = be.getBlockPos();

		TrackTargetingBehaviour<TrackObserver> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;

		// The overlay's placement is resolved by the track block against the live PoseStack, so the
		// inputs are carried across and the geometry is built during submission.
		state.renderOverlay = true;
		state.level = level;
		state.targetPosition = targetPosition;
		state.offset = targetPosition.subtract(pos);
		state.targetDirection = target.getTargetDirection();
		state.targetBezier = target.getTargetBezier();
	}

	@Override
	protected void submitSafe(TrackObserverRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (!state.renderOverlay || state.level == null || state.targetPosition == null)
			return;

		ms.pushPose();
		TransformStack.of(ms)
			.translate(state.offset);
		TrackTargetingBehaviour.submit(state.level, state.targetPosition, state.targetDirection, state.targetBezier,
			ms, queue, RenderedTrackOverlayType.OBSERVER, 1);
		ms.popPose();
	}

}
