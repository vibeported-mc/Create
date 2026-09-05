package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Transform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class StationRenderer extends SafeBlockEntityRenderer<StationBlockEntity, StationRenderer.StationRenderState> {

	public static class StationRenderState extends SafeRenderState {
		public final DepotRenderer.DepotRenderState depot = new DepotRenderer.DepotRenderState();
		public @Nullable SuperByteBufferRenderState flag;
		public final List<AssemblySlot> assembly = new ArrayList<>();
		public @Nullable Level level;
		public @Nullable BlockPos targetPosition;
		public @Nullable BlockPos offset;
		public @Nullable AxisDirection targetDirection;
		public @Nullable BezierTrackPointLocation targetBezier;
		public @Nullable RenderedTrackOverlayType overlayType;
	}

	/** One bogey slot of the assembly overlay, at `step` blocks along the track. */
	public record AssemblySlot(SuperByteBufferRenderState overlay, int step) {
	}

	protected final ItemModelResolver itemModelResolver;

	public StationRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public StationRenderState createRenderState() {
		return new StationRenderState();
	}

	@Override
	protected void extractSafe(StationBlockEntity be, StationRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.flag = null;
		state.assembly.clear();
		state.overlayType = null;

		BlockPos pos = be.getBlockPos();
		TrackTargetingBehaviour<GlobalStation> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();

		DepotRenderer.extractItemsOf(be, state.depot, partialTicks, be.depotBehaviour, itemModelResolver);

		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();
		if (!(block instanceof ITrackBlock track))
			return;

		GlobalStation station = be.getStation();
		boolean isAssembling = be.getBlockState()
			.getValue(StationBlock.ASSEMBLING);

		state.level = level;
		state.targetPosition = targetPosition;
		state.targetDirection = target.getTargetDirection();
		state.targetBezier = target.getTargetBezier();

		if (!isAssembling || (station == null || station.getPresentTrain() != null) && !be.isVirtual()) {
			state.flag = extractFlag(be.flag.getValue(partialTicks) > 0.75f ? AllPartialModels.STATION_ON
				: AllPartialModels.STATION_OFF, be, partialTicks, state.lightCoords);
			state.overlayType = RenderedTrackOverlayType.STATION;
			state.offset = targetPosition.subtract(pos);
			return;
		}

		state.flag = extractFlag(AllPartialModels.STATION_ASSEMBLE, be, partialTicks, state.lightCoords);

		Direction direction = be.assemblyDirection;

		if (be.isVirtual() && be.bogeyLocations == null)
			be.refreshAssemblyInfo();

		if (direction == null || be.assemblyLength == 0 || be.bogeyLocations == null)
			return;

		// The overlay model is prepared against a live PoseStack: preparing it points the overlay along
		// the track, so that pose has to be baked into each slot's buffer rather than just its offset.
		PoseStack ms = new PoseStack();
		BlockPos offset = targetPosition.subtract(pos);
		ms.translate(offset.getX(), offset.getY(), offset.getZ());

		MutableBlockPos currentPos = targetPosition.mutable();
		PartialModel assemblyOverlay = track.prepareAssemblyOverlay(level, targetPosition, trackState, direction, ms);
		int colorWhenValid = 0x96B5FF;
		int colorWhenCarriage = 0xCAFF96;

		currentPos.move(direction, 1);

		for (int i = 0; i < be.assemblyLength; i++) {
			int valid = be.isValidBogeyOffset(i) ? colorWhenValid : -1;

			for (int j : be.bogeyLocations)
				if (i == j) {
					valid = colorWhenCarriage;
					break;
				}

			if (valid != -1) {
				ms.pushPose();
				ms.translate(0, 0, i + 1);
				SuperByteBuffer sbb = CachedBufferer.partial(assemblyOverlay, trackState);
				sbb.transform(ms);
				sbb.color(valid);
				sbb.light(LightCoordsUtil.getLightCoords(level, currentPos));
				state.assembly.add(new AssemblySlot(sbb.extractRenderState(), i + 1));
				ms.popPose();
			}
			currentPos.move(direction);
		}
	}

	@Override
	protected void submitSafe(StationRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		DepotRenderer.submitItemsOf(state.depot, ms, queue, state.lightCoords);

		if (state.flag != null)
			state.flag.submit(ms, RenderTypes.cutoutMovingBlock(), queue);

		if (state.overlayType != null && state.level != null && state.targetPosition != null) {
			ms.pushPose();
			TransformStack.of(ms)
				.translate(state.offset);
			TrackTargetingClient.submitOverlay(state.level, state.targetPosition, state.targetDirection,
				state.targetBezier, ms, queue, state.overlayType, 1);
			ms.popPose();
			return;
		}

		if (state.assembly.isEmpty())
			return;

		for (AssemblySlot slot : state.assembly)
			slot.overlay()
				.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	public static SuperByteBufferRenderState extractFlag(PartialModel flag, StationBlockEntity be, float partialTicks,
		int light) {
		if (!be.resolveFlagAngle())
			return null;
		SuperByteBuffer flagBB = CachedBufferer.partial(flag, be.getBlockState());
		var tr = TransformStack.of(flagBB.getTransforms());
		transformFlag(tr, be, partialTicks, be.flagYRot, be.flagFlipped);
		tr.translate(0.5f / 16, 0, 0)
			.rotateYDegrees(be.flagFlipped ? 0 : 180)
			.translate(-0.5f / 16, 0, 0);
		return flagBB.light(light)
			.extractRenderState();
	}

	/**
	 * How far the flag has swung, 0 to 1, overshooting slightly while it settles.
	 */
	public static float flagProgress(StationBlockEntity be, float partialTicks) {
		float value = be.flag.getValue(partialTicks);
		float progress = (float) (Math.pow(Math.min(value * 5, 1), 2));
		if (be.flag.getChaseTarget() > 0 && !be.flag.settled() && progress == 1) {
			float wiggleProgress = (value - .2f) / .8f;
			progress += (Math.sin(wiggleProgress * (2 * Mth.PI) * 4) / 8f) / Math.max(1, 8f * wiggleProgress);
		}
		return progress;
	}

	public static void transformFlag(Transform<?> flag, StationBlockEntity be, float partialTicks, int yRot,
									 boolean flipped) {
		float progress = flagProgress(be, partialTicks);

		float nudge = 1 / 512f;
		flag.center()
			.rotateYDegrees(yRot)
			.translate(nudge, 9.5f / 16f, flipped ? 14f / 16f - nudge : 2f / 16f + nudge)
			.uncenter()
			.rotateXDegrees((flipped ? 1 : -1) * (progress * 90 + 270));
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96 * 2;
	}

}
