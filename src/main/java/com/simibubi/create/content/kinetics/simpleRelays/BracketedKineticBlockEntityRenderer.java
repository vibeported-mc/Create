package com.simibubi.create.content.kinetics.simpleRelays;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;

public class BracketedKineticBlockEntityRenderer
	extends KineticBlockEntityRenderer<BracketedKineticBlockEntity, BracketedKineticBlockEntityRenderer.BracketedRenderState> {

	public static class BracketedRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState cog;
		public @Nullable SuperByteBufferRenderState shaft;
	}

	public BracketedKineticBlockEntityRenderer(Context context) {
		super(context);
	}

	@Override
	public BracketedRenderState createRenderState() {
		return new BracketedRenderState();
	}

	@Override
	protected void extractSafe(BracketedKineticBlockEntity be, BracketedRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		if (!AllBlocks.LARGE_COGWHEEL.has(be.getBlockState())) {
			super.extractSafe(be, state, partialTicks, cameraPosition);
			return;
		}

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		// Large cogs sometimes have to offset their teeth by 11.25 degrees in order to
		// mesh properly

		state.renderType = RenderTypes.solidMovingBlock();
		Axis axis = getRotationAxisOf(be);
		Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
		state.cog = standardKineticRotationTransform(
			CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, be.getBlockState(), facing),
			be, state.lightCoords).extractRenderState();

		float angle = getAngleForLargeCogShaft(be, axis);
		SuperByteBuffer shaft =
			CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, be.getBlockState(), facing);
		state.shaft = kineticRotationTransform(shaft, be, axis, angle, state.lightCoords).extractRenderState();
	}

	@Override
	protected void submitSafe(BracketedRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.cog == null) {
			super.submitSafe(state, ms, queue, camera);
			return;
		}
		state.cog.submit(ms, state.renderType, queue);
		if (state.shaft != null)
			state.shaft.submit(ms, state.renderType, queue);
	}

	public static float getAngleForLargeCogShaft(SimpleKineticBlockEntity be, Axis axis) {
		BlockPos pos = be.getBlockPos();
		float offset = getShaftAngleOffset(axis, pos);
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
		return angle;
	}

	public static float getShaftAngleOffset(Axis axis, BlockPos pos) {
		if (KineticBlockEntityVisual.shouldOffset(axis, pos)) {
			return 22.5f;
		} else {
			return 0;
		}
	}

}
