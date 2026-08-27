package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ContraptionControlsRenderer
	extends SmartBlockEntityRenderer<ContraptionControlsBlockEntity, ContraptionControlsRenderer.ControlsRenderState> {

	public static class ControlsRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState button;
		public @Nullable SuperByteBufferRenderState indicator;
		public Vec3 buttonMovement = Vec3.ZERO;
		public Vec3 buttonOffset = Vec3.ZERO;
	}

	public ContraptionControlsRenderer(Context context) {
		super(context);
	}

	@Override
	public ControlsRenderState createRenderState() {
		return new ControlsRenderState();
	}

	@Override
	protected void extractSafe(ContraptionControlsBlockEntity be, ControlsRenderState state, float pt,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, pt, cameraPosition);

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(ContraptionControlsBlock.FACING)
			.getOpposite();
		Vec3 buttonMovementAxis = VecHelper.rotate(new Vec3(0, 1, -.325), AngleHelper.horizontalAngle(facing), Axis.Y);

		state.buttonMovement = buttonMovementAxis.scale(-0.07f + -1 / 24f * be.button.getValue(pt));
		state.buttonOffset = buttonMovementAxis.scale(0.07f);

		state.button = CachedBufferer.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, blockState, facing)
			.light(state.lightCoords)
			.extractRenderState();

		int i = (((int) be.indicator.getValue(pt) / 45) % 8) + 8;
		state.indicator = CachedBuffers
			.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_INDICATOR.get(i % 8), blockState, facing)
			.light(state.lightCoords)
			.extractRenderState();
	}

	/**
	 * The button, and the filter and link drawn by the smart renderer above it, sink together as the
	 * button is pressed; the indicator stays put.
	 */
	@Override
	protected void submitSafe(ControlsRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		ms.pushPose();
		ms.translate(state.buttonMovement.x, state.buttonMovement.y, state.buttonMovement.z);
		super.submitSafe(state, ms, queue, camera);
		ms.translate(state.buttonOffset.x, state.buttonOffset.y, state.buttonOffset.z);

		if (state.button != null)
			state.button.submit(ms, RenderTypes.solidMovingBlock(), queue);
		ms.popPose();

		if (state.indicator != null)
			state.indicator.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public static void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld,
										   ContraptionMatrices matrices, SubmitNodeCollector buffer) {

		if (!(ctx.temporaryData instanceof ElevatorFloorSelection efs))
			return;
		if (!AllBlocks.CONTRAPTION_CONTROLS.has(ctx.state))
			return;

		Entity cameraEntity = Minecraft.getInstance()
			.getCameraEntity();
		float playerDistance = (float) (ctx.position == null || cameraEntity == null ? 0
			: ctx.position.distanceToSqr(cameraEntity.getEyePosition()));

		float flicker = renderWorld.getRandom().nextFloat();
		Couple<Integer> couple = DyeHelper.getDyeColors(efs.targetYEqualsSelection ? DyeColor.WHITE : DyeColor.ORANGE);
		int brightColor = couple.getFirst();
		int darkColor = couple.getSecond();
		int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4);
		Font fontRenderer = Minecraft.getInstance().font;
		float shadowOffset = .5f;

		String text = efs.currentShortName;
		String description = efs.currentLongName;
		PoseStack ms = matrices.getViewProjection();
		var msr = TransformStack.of(ms);

		float buttondepth = 0;
		if (ctx.contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
			buttondepth = -1 / 24f * cbe.button.getValue(AnimationTickHolder.getPartialTicks(renderWorld));

		ms.pushPose();
		msr.translate(ctx.localPos);
		ms.translate(0, buttondepth, 0);
		CachedBufferer.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, ctx.state, ctx.state.getValue(ContraptionControlsBlock.FACING).getOpposite())
			.light(LightCoordsUtil.getLightCoords(renderWorld, ctx.localPos))
			.useLevelLight(ctx.world, matrices.getWorld())
			.submit(ms, RenderTypes.solidMovingBlock(), buffer);
		ms.popPose();

		ms.pushPose();
		msr.translate(ctx.localPos);
		msr.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(ctx.state.getValue(ContraptionControlsBlock.FACING))),
			Direction.UP);
		ms.translate(0.275f + 0.125f, 1 + 2 / 16f, 0.5f);
		msr.rotate(AngleHelper.rad(67.5f), Direction.WEST);

		if (!text.isBlank() && playerDistance < 100) {
			int actualWidth = fontRenderer.width(text);
			int width = Math.max(actualWidth, 12);
			float scale = 1 / (5f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.pushPose();
			ms.translate(0, .15f, buttondepth - .25f);
			ms.scale(scale, -scale, scale);
			ms.translate((float) Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.submitInWorldString(ms, buffer, text, flickeringBrightColor);
			ms.translate(shadowOffset, shadowOffset, -1 / 16f);
			NixieTubeRenderer.submitInWorldString(ms, buffer, text, Color.mixColors(darkColor, 0, .35f));
			ms.popPose();
		}

		if (!description.isBlank() && playerDistance < 20) {
			int actualWidth = fontRenderer.width(description);
			int width = Math.max(actualWidth, 55);
			float scale = 1 / (3f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.pushPose();
			ms.translate(-.0635f, 0.06f, buttondepth - .25f);
			ms.scale(scale, -scale, scale);
			ms.translate((float) Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.submitInWorldString(ms, buffer, description, flickeringBrightColor);
			ms.popPose();
		}

		ms.popPose();

	}
}
