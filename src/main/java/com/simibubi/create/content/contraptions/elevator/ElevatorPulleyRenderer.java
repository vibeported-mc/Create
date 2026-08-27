package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ElevatorPulleyRenderer
	extends KineticBlockEntityRenderer<ElevatorPulleyBlockEntity, ElevatorPulleyRenderer.ElevatorPulleyRenderState> {

	public static class ElevatorPulleyRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
	}

	public ElevatorPulleyRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ElevatorPulleyRenderState createRenderState() {
		return new ElevatorPulleyRenderState();
	}

	@Override
	protected void extractSafe(ElevatorPulleyBlockEntity be, ElevatorPulleyRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.parts.clear();

		float offset = PulleyRenderer.getBlockEntityOffset(partialTicks, be);
		boolean running = PulleyRenderer.isPulleyRunning(be);

		SpriteShiftEntry beltShift = AllSpriteShifts.ELEVATOR_BELT;
		SpriteShiftEntry coilShift = AllSpriteShifts.ELEVATOR_COIL;
		Level world = be.getLevel();
		BlockState blockState = be.getBlockState();
		BlockPos pos = be.getBlockPos();

		float blockStateAngle =
			180 + AngleHelper.horizontalAngle(blockState.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING));

		if (running || offset == 0) {
			SuperByteBuffer magnet = CachedBufferer.partial(AllPartialModels.ELEVATOR_MAGNET, blockState);
			TransformStack.of(magnet.getTransforms())
				.center()
				.rotateYDegrees(blockStateAngle)
				.uncenter();
			state.parts.add(AbstractPulleyRenderer.extractAt(world, magnet, offset, pos));
		}

		SuperByteBuffer rotatedCoil = getRotatedCoil(be);
		if (offset == 0) {
			state.parts.add(rotatedCoil.light(state.lightCoords)
				.extractRenderState());
			return;
		}

		state.parts.add(AbstractPulleyRenderer.scrollCoil(rotatedCoil, coilShift, offset, 2)
			.light(state.lightCoords)
			.extractRenderState());

		float spriteSize = beltShift.getTarget()
			.getV1()
			- beltShift.getTarget()
				.getV0();

		double beltScroll = (-(offset + .5) - Math.floor(-(offset + .5))) / 2;

		float f = offset % 1;
		if (f < .25f || f > .75f) {
			SuperByteBuffer halfRope = CachedBufferer.partial(AllPartialModels.ELEVATOR_BELT_HALF, blockState);
			TransformStack.of(halfRope.getTransforms())
				.center()
				.rotateYDegrees(blockStateAngle)
				.uncenter();
			state.parts.add(AbstractPulleyRenderer.extractAt(world,
				halfRope.shiftUVScrolling(beltShift, (float) beltScroll * spriteSize), f > .75f ? f - 1 : f, pos));
		}

		if (!running)
			return;

		// Each belt segment sits at a different height, so it needs a buffer of its own.
		for (int i = 0; i < offset - .25f; i++) {
			SuperByteBuffer rope = CachedBufferer.partial(AllPartialModels.ELEVATOR_BELT, blockState);
			TransformStack.of(rope.getTransforms())
				.center()
				.rotateYDegrees(blockStateAngle)
				.uncenter();
			state.parts.add(AbstractPulleyRenderer.extractAt(world,
				rope.shiftUVScrolling(beltShift, (float) beltScroll * spriteSize), offset - i, pos));
		}
	}

	@Override
	protected void submitSafe(ElevatorPulleyRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected BlockState getRenderedBlockState(ElevatorPulleyBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	protected SuperByteBuffer getRotatedCoil(KineticBlockEntity be) {
		BlockState blockState = be.getBlockState();
		return CachedBufferer.partialFacing(AllPartialModels.ELEVATOR_COIL, blockState,
			blockState.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING));
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

}
