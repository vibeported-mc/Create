package com.simibubi.create.content.kinetics.steamEngine;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
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

public class SteamEngineRenderer
	extends SafeBlockEntityRenderer<SteamEngineBlockEntity, SteamEngineRenderer.SteamEngineRenderState> {

	public static class SteamEngineRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState piston;
		public @Nullable SuperByteBufferRenderState linkage;
		public @Nullable SuperByteBufferRenderState connector;
	}

	public SteamEngineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public SteamEngineRenderState createRenderState() {
		return new SteamEngineRenderState();
	}

	@Override
	protected void extractSafe(SteamEngineBlockEntity be, SteamEngineRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.piston = null;
		state.linkage = null;
		state.connector = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		Float angle = be.getTargetAngle();
		if (angle == null)
			return;

		BlockState blockState = be.getBlockState();
		Direction facing = SteamEngineBlock.getFacing(blockState);
		Axis facingAxis = facing.getAxis();
		Axis axis = Axis.Y;

		PoweredShaftBlockEntity shaft = be.getShaft();
		if (shaft != null)
			axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

		boolean roll90 = facingAxis.isHorizontal() && axis == Axis.Y || facingAxis.isVertical() && axis == Axis.Z;
		float piston = ((6 / 16f) * Mth.sin(angle)
			- Mth.sqrt(Mth.square(14 / 16f) - Mth.square(6 / 16f) * Mth.square(Mth.cos(angle))));
		float distance = Mth.sqrt(Mth.square(piston - 6 / 16f * Mth.sin(angle)));
		float angle2 = (float) Math.acos(distance / (14 / 16f)) * (Mth.cos(angle) >= 0 ? 1f : -1f);

		SuperByteBuffer pistonBuffer = transformed(AllPartialModels.ENGINE_PISTON, blockState, facing, roll90);
		TransformStack.of(pistonBuffer.getTransforms())
			.translate(0, piston + 20 / 16f, 0);
		state.piston = pistonBuffer.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer linkageBuffer = transformed(AllPartialModels.ENGINE_LINKAGE, blockState, facing, roll90);
		TransformStack.of(linkageBuffer.getTransforms())
			.center()
			.translate(0, 1, 0)
			.uncenter()
			.translate(0, piston + 20 / 16f, 0)
			.translate(0, 4 / 16f, 8 / 16f)
			.rotateX(angle2)
			.translate(0, -4 / 16f, -8 / 16f);
		state.linkage = linkageBuffer.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer connectorBuffer = transformed(AllPartialModels.ENGINE_CONNECTOR, blockState, facing, roll90);
		TransformStack.of(connectorBuffer.getTransforms())
			.translate(0, 2, 0)
			.center()
			.rotateX(-(angle + Mth.HALF_PI))
			.uncenter();
		state.connector = connectorBuffer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(SteamEngineRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.piston != null)
			state.piston.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.linkage != null)
			state.linkage.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.connector != null)
			state.connector.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	private SuperByteBuffer transformed(PartialModel model, BlockState blockState, Direction facing, boolean roll90) {
		SuperByteBuffer buffer = CachedBuffers.partial(model, blockState);
		TransformStack.of(buffer.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.rotateYDegrees(roll90 ? -90 : 0)
			.uncenter();
		return buffer;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

}
