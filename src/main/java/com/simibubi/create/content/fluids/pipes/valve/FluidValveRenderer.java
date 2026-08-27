package com.simibubi.create.content.fluids.pipes.valve;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FluidValveRenderer
	extends KineticBlockEntityRenderer<FluidValveBlockEntity, FluidValveRenderer.FluidValveRenderState> {

	public static class FluidValveRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState pointer;
	}

	public FluidValveRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public FluidValveRenderState createRenderState() {
		return new FluidValveRenderState();
	}

	@Override
	protected void extractSafe(FluidValveBlockEntity be, FluidValveRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.pointer = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		super.extractSafe(be, state, partialTicks, cameraPosition);

		BlockState blockState = be.getBlockState();
		SuperByteBuffer pointer = CachedBufferer.partial(AllPartialModels.FLUID_VALVE_POINTER, blockState);
		Direction facing = blockState.getValue(FluidValveBlock.FACING);

		float pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0, -90);
		Axis pipeAxis = FluidValveBlock.getPipeAxis(blockState);
		Axis shaftAxis = getRotationAxisOf(be);

		int pointerRotationOffset = 0;
		if (pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical())
			pointerRotationOffset = 90;

		TransformStack.of(pointer.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
			.rotateYDegrees(pointerRotationOffset + pointerRotation)
			.uncenter();
		state.pointer = pointer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(FluidValveRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.pointer != null)
			state.pointer.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected BlockState getRenderedBlockState(FluidValveBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
