package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GantryCarriageRenderer
	extends KineticBlockEntityRenderer<GantryCarriageBlockEntity, GantryCarriageRenderer.GantryCarriageRenderState> {

	public static class GantryCarriageRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState cogs;
	}

	public GantryCarriageRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public GantryCarriageRenderState createRenderState() {
		return new GantryCarriageRenderState();
	}

	@Override
	protected void extractSafe(GantryCarriageBlockEntity be, GantryCarriageRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.cogs = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(GantryCarriageBlock.FACING);
		Boolean alongFirst = blockState.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
		Axis rotationAxis = getRotationAxisOf(be);
		BlockPos visualPos = facing.getAxisDirection() == AxisDirection.POSITIVE ? be.getBlockPos()
			: be.getBlockPos()
				.relative(facing.getOpposite());
		float angleForBE = getAngleForBE(be, visualPos, rotationAxis);

		Axis gantryAxis = Axis.X;
		for (Axis axis : Iterate.axes)
			if (axis != rotationAxis && axis != facing.getAxis())
				gantryAxis = axis;

		if (gantryAxis == Axis.X)
			if (facing == Direction.UP)
				angleForBE *= -1;
		if (gantryAxis == Axis.Y)
			if (facing == Direction.NORTH || facing == Direction.EAST)
				angleForBE *= -1;

		SuperByteBuffer cogs = CachedBufferer.partial(AllPartialModels.GANTRY_COGS, blockState);
		TransformStack.of(cogs.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
			.rotateYDegrees(alongFirst ^ facing.getAxis() == Axis.X ? 0 : 90)
			.translate(0, -9 / 16f, 0)
			.rotateXDegrees(-angleForBE)
			.translate(0, 9 / 16f, 0)
			.uncenter();

		state.cogs = cogs.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(GantryCarriageRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.cogs != null)
			state.cogs.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public static float getAngleForBE(KineticBlockEntity be, final BlockPos pos, Axis axis) {
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float offset = getRotationOffsetForPosition(be, pos, axis);
		return (time * be.getSpeed() * 3f / 20 + offset) % 360;
	}

	@Override
	protected BlockState getRenderedBlockState(GantryCarriageBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
