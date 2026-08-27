package com.simibubi.create.content.redstone.deskBell;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DeskBellRenderer
	extends SmartBlockEntityRenderer<DeskBellBlockEntity, DeskBellRenderer.DeskBellRenderState> {

	public static class DeskBellRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState plunger;
		public @Nullable SuperByteBufferRenderState bell;
	}

	public DeskBellRenderer(Context context) {
		super(context);
	}

	@Override
	public DeskBellRenderState createRenderState() {
		return new DeskBellRenderState();
	}

	@Override
	protected void extractSafe(DeskBellBlockEntity be, DeskBellRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.plunger = null;
		state.bell = null;

		BlockState blockState = be.getBlockState();
		float p = be.animation.getValue(partialTicks);
		if (p < 0.004 && !blockState.getOptionalValue(DeskBellBlock.POWERED)
			.orElse(false))
			return;

		float f = (float) (1 - 4 * Math.pow((Math.max(p - 0.5, 0)) - 0.5, 2));
		float f2 = (float) (Math.pow(p, 1.25f));

		Direction facing = blockState.getValue(DeskBellBlock.FACING);

		SuperByteBuffer plunger = CachedBuffers.partial(AllPartialModels.DESK_BELL_PLUNGER, blockState);
		TransformStack.of(plunger.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.uncenter()
			.translate(0, f * -.75f / 16f, 0);
		state.plunger = plunger.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer bell = CachedBuffers.partial(AllPartialModels.DESK_BELL_BELL, blockState);
		TransformStack.of(bell.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.translate(0, -1 / 16, 0)
			.rotateXDegrees(f2 * 8 * Mth.sin(p * Mth.PI * 4 + be.animationOffset))
			.rotateZDegrees(f2 * 8 * Mth.cos(p * Mth.PI * 4 + be.animationOffset))
			.translate(0, 1 / 16, 0)
			.scale(0.995f)
			.uncenter();
		state.bell = bell.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(DeskBellRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.plunger != null)
			state.plunger.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.bell != null)
			state.bell.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
