package com.simibubi.create.content.decoration.slidingDoor;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public class SlidingDoorRenderer
	extends SafeBlockEntityRenderer<SlidingDoorBlockEntity, SlidingDoorRenderer.SlidingDoorRenderState> {

	public static class SlidingDoorRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(2);
	}

	public SlidingDoorRenderer(Context context) {
	}

	@Override
	public SlidingDoorRenderState createRenderState() {
		return new SlidingDoorRenderState();
	}

	@Override
	protected void extractSafe(SlidingDoorBlockEntity be, SlidingDoorRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.parts.clear();

		BlockState blockState = be.getBlockState();
		if (!be.shouldRenderSpecial(blockState))
			return;

		Direction facing = blockState.getValue(DoorBlock.FACING);
		Direction movementDirection = facing.getClockWise();

		if (blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT)
			movementDirection = movementDirection.getOpposite();

		float value = be.animation.getValue(partialTicks);
		float value2 = Mth.clamp(value * 10, 0, 1);

		Vec3 offset = Vec3.atLowerCornerOf(movementDirection.getUnitVec3i())
			.scale(value * value * 13 / 16f)
			.add(Vec3.atLowerCornerOf(facing.getUnitVec3i())
				.scale(value2 * 1 / 32f));

		if (((SlidingDoorBlock) blockState.getBlock()).isFoldingDoor()) {
			Couple<PartialModel> partials =
				AllPartialModels.FOLDING_DOORS.get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));

			boolean flip = blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
			for (boolean left : Iterate.trueAndFalse) {
				SuperByteBuffer partial = CachedBuffers.partial(partials.get(left ^ flip), blockState);
				float f = flip ? -1 : 1;

				var msr = TransformStack.of(partial.getTransforms());
				msr.translate(0, -1 / 512f, 0)
					.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
						.scale(value2 * 1 / 32f));
				msr.rotateCentered(Mth.DEG_TO_RAD * AngleHelper.horizontalAngle(facing.getClockWise()), Direction.UP);

				if (flip)
					msr.translate(0, 0, 1);
				msr.rotateYDegrees(91 * f * value * value);

				if (!left)
					msr.translate(0, 0, f / 2f)
						.rotateYDegrees(-181 * f * value * value);

				if (flip)
					msr.translate(0, 0, -1 / 2f);

				state.parts.add(partial.light(state.lightCoords)
					.extractRenderState());
			}

			return;
		}

		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			SuperByteBuffer partial = CachedBuffers.block(blockState.setValue(DoorBlock.OPEN, false)
				.setValue(DoorBlock.HALF, half));
			TransformStack.of(partial.getTransforms())
				.translate(0, half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0, 0)
				.translate(offset);
			state.parts.add(partial.light(state.lightCoords)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(SlidingDoorRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

}
