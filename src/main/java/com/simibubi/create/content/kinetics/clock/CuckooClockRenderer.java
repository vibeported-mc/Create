package com.simibubi.create.content.kinetics.clock;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.clock.CuckooClockBlockEntity.Animation;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class CuckooClockRenderer
	extends KineticBlockEntityRenderer<CuckooClockBlockEntity, CuckooClockRenderer.CuckooClockRenderState> {

	public static class CuckooClockRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(5);
	}

	public CuckooClockRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public CuckooClockRenderState createRenderState() {
		return new CuckooClockRenderState();
	}

	@Override
	protected void extractSafe(CuckooClockBlockEntity be, CuckooClockRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.parts.clear();

		BlockState blockState = be.getBlockState();
		Direction direction = blockState.getValue(CuckooClockBlock.HORIZONTAL_FACING);

		// Render Hands
		SuperByteBuffer hourHand = CachedBufferer.partial(AllPartialModels.CUCKOO_HOUR_HAND, blockState);
		SuperByteBuffer minuteHand = CachedBufferer.partial(AllPartialModels.CUCKOO_MINUTE_HAND, blockState);
		float hourAngle = be.hourHand.getValue(partialTicks);
		float minuteAngle = be.minuteHand.getValue(partialTicks);
		state.parts.add(rotateHand(hourHand, hourAngle, direction).light(state.lightCoords)
			.extractRenderState());
		state.parts.add(rotateHand(minuteHand, minuteAngle, direction).light(state.lightCoords)
			.extractRenderState());

		// Doors
		SuperByteBuffer leftDoor = CachedBufferer.partial(AllPartialModels.CUCKOO_LEFT_DOOR, blockState);
		SuperByteBuffer rightDoor = CachedBufferer.partial(AllPartialModels.CUCKOO_RIGHT_DOOR, blockState);
		float angle = 0;
		float offset = 0;

		if (be.animationType != null) {
			float value = be.animationProgress.getValue(partialTicks);
			int step = be.animationType == Animation.SURPRISE ? 3 : 15;
			for (int phase = 30; phase <= 60; phase += step) {
				float local = value - phase;
				if (local < -step / 3)
					continue;
				else if (local < 0)
					angle = Mth.lerp(((value - (phase - 5)) / 5), 0, 135);
				else if (local < step / 3)
					angle = 135;
				else if (local < 2 * step / 3)
					angle = Mth.lerp(((value - (phase + 5)) / 5), 135, 0);

			}
		}

		state.parts.add(rotateDoor(leftDoor, angle, true, direction).light(state.lightCoords)
			.extractRenderState());
		state.parts.add(rotateDoor(rightDoor, angle, false, direction).light(state.lightCoords)
			.extractRenderState());

		// Figure
		if (be.animationType != Animation.NONE) {
			offset = -(angle / 135) * 1 / 2f + 10 / 16f;
			PartialModel partialModel =
				(be.animationType == Animation.PIG ? AllPartialModels.CUCKOO_PIG : AllPartialModels.CUCKOO_CREEPER);
			SuperByteBuffer figure = CachedBufferer.partial(partialModel, blockState);
			TransformStack.of(figure.getTransforms())
				.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(direction.getCounterClockWise())),
					Direction.UP)
				.translate(offset, 0, 0);
			state.parts.add(figure.light(state.lightCoords)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(CuckooClockRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(CuckooClockBlockEntity be, BlockState state) {
		return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, state, state
				.getValue(CuckooClockBlock.HORIZONTAL_FACING)
				.getOpposite());
	}

	private SuperByteBuffer rotateHand(SuperByteBuffer buffer, float angle, Direction facing) {
		float pivotX = 2 / 16f;
		float pivotY = 6 / 16f;
		float pivotZ = 8 / 16f;
		TransformStack.of(buffer.getTransforms())
			.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getCounterClockWise())), Direction.UP)
			.translate(pivotX, pivotY, pivotZ)
			.rotate(AngleHelper.rad(angle), Direction.EAST)
			.translate(-pivotX, -pivotY, -pivotZ);
		return buffer;
	}

	private SuperByteBuffer rotateDoor(SuperByteBuffer buffer, float angle, boolean left, Direction facing) {
		float pivotX = 2 / 16f;
		float pivotY = 0;
		float pivotZ = (left ? 6 : 10) / 16f;
		TransformStack.of(buffer.getTransforms())
			.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getCounterClockWise())), Direction.UP)
			.translate(pivotX, pivotY, pivotZ)
			.rotate(AngleHelper.rad(angle) * (left ? -1 : 1), Direction.UP)
			.translate(-pivotX, -pivotY, -pivotZ);
		return buffer;
	}

}
