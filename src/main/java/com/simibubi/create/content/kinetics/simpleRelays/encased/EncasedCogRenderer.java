package com.simibubi.create.content.kinetics.simpleRelays.encased;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
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
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EncasedCogRenderer
	extends KineticBlockEntityRenderer<SimpleKineticBlockEntity, EncasedCogRenderer.EncasedCogRenderState> {

	public static class EncasedCogRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> shafts = new ArrayList<>(2);
	}

	private boolean large;

	public static EncasedCogRenderer small(BlockEntityRendererProvider.Context context) {
		return new EncasedCogRenderer(context, false);
	}

	public static EncasedCogRenderer large(BlockEntityRendererProvider.Context context) {
		return new EncasedCogRenderer(context, true);
	}

	public EncasedCogRenderer(BlockEntityRendererProvider.Context context, boolean large) {
		super(context);
		this.large = large;
	}

	@Override
	public EncasedCogRenderState createRenderState() {
		return new EncasedCogRenderState();
	}

	@Override
	protected void extractSafe(SimpleKineticBlockEntity be, EncasedCogRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.shafts.clear();
		super.extractSafe(be, state, partialTicks, cameraPosition);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		Block block = blockState.getBlock();
		if (!(block instanceof IRotate def))
			return;

		Axis axis = getRotationAxisOf(be);
		BlockPos pos = be.getBlockPos();
		float angle = large ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft(be, axis)
			: getAngleForBe(be, pos, axis);

		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!def.hasShaftTowards(be.getLevel(), be.getBlockPos(), blockState, d))
				continue;
			SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, blockState, d);
			kineticRotationTransform(shaft, be, axis, angle, state.lightCoords);
			state.shafts.add(shaft.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(EncasedCogRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState shaft : state.shafts)
			shaft.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(SimpleKineticBlockEntity be, BlockState state) {
		return CachedBufferer.partialFacingVertical(
			large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL, state,
			Direction.fromAxisAndDirection(state.getValue(EncasedCogwheelBlock.AXIS), AxisDirection.POSITIVE));
	}

}
