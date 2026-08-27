package com.simibubi.create.content.kinetics.gearbox;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class GearboxRenderer
	extends KineticBlockEntityRenderer<GearboxBlockEntity, GearboxRenderer.GearboxRenderState> {

	public static class GearboxRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> shafts = new ArrayList<>(4);
	}

	public GearboxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public GearboxRenderState createRenderState() {
		return new GearboxRenderState();
	}

	@Override
	protected void extractSafe(GearboxBlockEntity be, GearboxRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		// The gearbox draws four shaft halves instead of the inherited kinetic model.
		state.model = null;
		state.shafts.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		final Axis boxAxis = be.getBlockState().getValue(BlockStateProperties.AXIS);
		final BlockPos pos = be.getBlockPos();
		float time = AnimationTickHolder.getRenderTime(be.getLevel());

		for (Direction direction : Iterate.directions) {
			final Axis axis = direction.getAxis();
			if (boxAxis == axis)
				continue;

			SuperByteBuffer shaft =
				CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
			float offset = getRotationOffsetForPosition(be, pos, axis);
			float angle = (time * be.getSpeed() * 3f / 10) % 360;

			if (be.getSpeed() != 0 && be.hasSource()) {
				BlockPos source = be.source.subtract(be.getBlockPos());
				Direction sourceFacing = Direction.getApproximateNearest(source.getX(), source.getY(), source.getZ());
				if (sourceFacing.getAxis() == direction.getAxis())
					angle *= sourceFacing == direction ? 1 : -1;
				else if (sourceFacing.getAxisDirection() == direction.getAxisDirection())
					angle *= -1;
			}

			angle += offset;
			angle = angle / 180f * (float) Math.PI;

			kineticRotationTransform(shaft, be, axis, angle, state.lightCoords);
			state.shafts.add(shaft.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(GearboxRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState shaft : state.shafts)
			shaft.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
