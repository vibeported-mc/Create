package com.simibubi.create.content.kinetics.transmission;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class SplitShaftRenderer
	extends KineticBlockEntityRenderer<SplitShaftBlockEntity, SplitShaftRenderer.SplitShaftRenderState> {

	public static class SplitShaftRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> halves = new ArrayList<>(2);
	}

	public SplitShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public SplitShaftRenderState createRenderState() {
		return new SplitShaftRenderState();
	}

	@Override
	protected void extractSafe(SplitShaftBlockEntity be, SplitShaftRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		// This renderer replaces the base kinetic model entirely rather than adding to it.
		state.model = null;
		state.halves.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		Block block = be.getBlockState().getBlock();
		final Axis boxAxis = ((IRotate) block).getRotationAxis(be.getBlockState());
		final BlockPos pos = be.getBlockPos();
		float time = AnimationTickHolder.getRenderTime(be.getLevel());

		for (Direction direction : Iterate.directions) {
			Axis axis = direction.getAxis();
			if (boxAxis != axis)
				continue;

			float offset = getRotationOffsetForPosition(be, pos, axis);
			float angle = (time * be.getSpeed() * 3f / 10) % 360;
			float modifier = be.getRotationSpeedModifier(direction);

			angle *= modifier;
			angle += offset;
			angle = angle / 180f * (float) Math.PI;

			SuperByteBuffer superByteBuffer =
				CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
			kineticRotationTransform(superByteBuffer, be, axis, angle, state.lightCoords);
			state.halves.add(superByteBuffer.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(SplitShaftRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState half : state.halves)
			half.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
