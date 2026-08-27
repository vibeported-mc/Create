package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity.ComputerSignal;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import org.jspecify.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.redstone.nixieTube.DoubleFaceAttachedBlock.DoubleAttachFace;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.DyeHelper;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NixieTubeRenderer
	extends SafeBlockEntityRenderer<NixieTubeBlockEntity, NixieTubeRenderer.NixieRenderState> {

	/**
	 * The geometry here is cheap cache lookups whose transforms interleave with the PoseStack, so
	 * extraction only captures what is read off the block entity and the buffers are built and
	 * submitted together later. Nothing in the submit path touches the block entity.
	 */
	public static class NixieRenderState extends SafeRenderState {
		public @Nullable Couple<String> displayedStrings;
		public DyeColor color = DyeColor.WHITE;
		public @Nullable SignalState signalState;
		public @Nullable ComputerSignal computerSignal;
		public Vec3 observerVec = Vec3.ZERO;
		public Vec3 lampVec = Vec3.ZERO;
		public float renderTime;
		public long randomSeed;
	}

	private static final int GLOW_VIEW_DISTANCE = 96;

	public NixieTubeRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public NixieRenderState createRenderState() {
		return new NixieRenderState();
	}

	@Override
	protected void extractSafe(NixieTubeBlockEntity be, NixieRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.signalState = be.signalState;
		state.computerSignal = be.computerSignal;
		state.observerVec = cameraPosition;
		state.lampVec = Vec3.atCenterOf(be.getBlockPos());
		state.renderTime = AnimationTickHolder.getRenderTime(be.getLevel());
		state.randomSeed = be.getBlockPos()
			.asLong();

		if (state.signalState == null && state.computerSignal == null) {
			state.displayedStrings = be.getDisplayedStrings();
			state.color = NixieTubeBlock.colorOf(be.getBlockState());
		}
	}

	@Override
	protected void submitSafe(NixieRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		ms.pushPose();
		BlockState blockState = state.blockState;
		DoubleAttachFace face = blockState.getValue(NixieTubeBlock.FACE);
		float yRot = AngleHelper.horizontalAngle(blockState.getValue(NixieTubeBlock.FACING)) - 90
			+ (face == DoubleAttachFace.WALL_REVERSED ? 180 : 0);
		float xRot = face == DoubleAttachFace.WALL ? -90 : face == DoubleAttachFace.WALL_REVERSED ? 90 : 0;

		var msr = TransformStack.of(ms);
		msr.center()
			.rotateYDegrees(yRot)
			.rotateZDegrees(xRot)
			.uncenter();

		if (state.signalState != null || state.computerSignal != null) {
			submitAsSignal(state, ms, queue);
			ms.popPose();
			return;
		}

		msr.center();

		float height = face == DoubleAttachFace.CEILING ? 5 : 3;
		float scale = 1 / 20f;

		Couple<String> strings = state.displayedStrings;
		// The flicker was driven by the level's shared random; a position-seeded one keeps each tube
		// flickering independently without reaching back into the level.
		RandomSource random = RandomSource.create(state.randomSeed);

		ms.pushPose();
		ms.translate(-4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		submitTube(ms, queue, strings.getFirst(), height, state.color, random);
		ms.popPose();

		ms.pushPose();
		ms.translate(4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		submitTube(ms, queue, strings.getSecond(), height, state.color, random);
		ms.popPose();

		ms.popPose();
	}

	public static void submitTube(PoseStack ms, SubmitNodeCollector queue, String c, float height, DyeColor color, RandomSource random) {
		Font fontRenderer = Minecraft.getInstance().font;
		float charWidth = fontRenderer.width(c);
		float shadowOffset = .5f;
		float flicker = random.nextFloat();
		Couple<Integer> couple = DyeHelper.getDyeColors(color);
		int brightColor = couple.getFirst();
		int darkColor = couple.getSecond();
		int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4);

		ms.pushPose();
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		submitInWorldString(ms, queue, c, flickeringBrightColor);
		ms.pushPose();
		ms.translate(shadowOffset, shadowOffset, -1 / 16f);
		submitInWorldString(ms, queue, c, darkColor);
		ms.popPose();
		ms.popPose();

		ms.pushPose();
		ms.scale(-1, 1, 1);
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		submitInWorldString(ms, queue, c, darkColor);
		ms.pushPose();
		ms.translate(-shadowOffset, shadowOffset, -1 / 16f);
		submitInWorldString(ms, queue, c, Color.mixColors(darkColor, 0, .35f));
		ms.popPose();
		ms.popPose();
	}

	/**
	 * The queue owns glyph batching now, so the manual endBatch the old code needed to keep in-world
	 * text from bleeding into later draws is gone.
	 */
	public static void submitInWorldString(PoseStack ms, SubmitNodeCollector queue, String c, int color) {
		queue.submitText(ms, 0, 0, Component.literal(c)
			.getVisualOrderText(), false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
	}

	private void submitAsSignal(NixieRenderState state, PoseStack ms, SubmitNodeCollector queue) {
		BlockState blockState = state.blockState;
		int light = state.lightCoords;
		Direction facing = NixieTubeBlock.getFacing(blockState);
		Vec3 observerVec = state.observerVec;
		var msr = TransformStack.of(ms);

		if (facing == Direction.DOWN)
			msr.center()
				.rotateZDegrees(180)
				.uncenter();

		boolean invertTubes =
			facing == Direction.DOWN || blockState.getValue(NixieTubeBlock.FACE) == DoubleAttachFace.WALL_REVERSED;

		CachedBufferer.partial(AllPartialModels.SIGNAL_PANEL, blockState)
			.light(light)
			.submit(ms, net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock(), queue);

		ms.pushPose();
		ms.translate(1 / 2f, 7.5f / 16f, 1 / 2f);
		float renderTime = state.renderTime;
		Vec3 diff = state.lampVec.subtract(observerVec);

		if (state.signalState != null) {
			for (boolean first : Iterate.trueAndFalse) {
				if (first && !state.signalState.isRedLight(renderTime))
					continue;
				if (!first && !state.signalState.isGreenLight(renderTime) && !state.signalState.isYellowLight(renderTime))
					continue;

				boolean flip = first == invertTubes;
				boolean yellow = state.signalState.isYellowLight(renderTime);

				ms.pushPose();
				ms.translate(flip ? 4 / 16f : -4 / 16f, 0, 0);

				if (diff.lengthSqr() < GLOW_VIEW_DISTANCE * GLOW_VIEW_DISTANCE) {
					boolean vert = first ^ facing.getAxis()
						.isHorizontal();
					float longSide = yellow ? 1 : 4;
					float longSideGlow = yellow ? 2 : 5.125f;

					submitScaled(CachedBufferer.partial(AllPartialModels.SIGNAL_WHITE_CUBE, blockState)
						.light(0xf000f0)
						.disableDiffuse(), ms, queue,
						net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock(),
						vert ? longSide : 1, vert ? 1 : longSide, 1);

					submitScaled(CachedBufferer
						.partial(
							first ? AllPartialModels.SIGNAL_RED_GLOW
								: yellow ? AllPartialModels.SIGNAL_YELLOW_GLOW : AllPartialModels.SIGNAL_WHITE_GLOW,
							blockState)
						.light(0xf000f0)
						.disableDiffuse(), ms, queue, RenderTypes.additive(), vert ? longSideGlow : 2, vert ? 2 : longSideGlow, 2);
				}

				submitScaled(CachedBufferer
					.partial(first ? AllPartialModels.SIGNAL_RED
						: yellow ? AllPartialModels.SIGNAL_YELLOW : AllPartialModels.SIGNAL_WHITE, blockState)
					.light(0xF000F0)
					.disableDiffuse(), ms, queue, RenderTypes.additive(), 1 + 1 / 16f);

				ms.popPose();
			}
		} else if (state.computerSignal != null) {
			for (boolean first : Iterate.trueAndFalse) {
				NixieTubeBlockEntity.ComputerSignal.TubeDisplay tubeDisplay = first ?
					state.computerSignal.first : state.computerSignal.second;
				if (tubeDisplay.blinkPeriod == 0 || tubeDisplay.blinkPeriod > 1 && renderTime % tubeDisplay.blinkPeriod < tubeDisplay.blinkOffTime)
					continue;

				boolean flip = first == invertTubes;

				ms.pushPose();
				ms.translate(flip ? 4 / 16f : -4 / 16f, 0, 0);

				if (diff.lengthSqr() < GLOW_VIEW_DISTANCE * GLOW_VIEW_DISTANCE) {
					boolean horiz = facing.getAxis().isHorizontal();
					float width = horiz ? tubeDisplay.glowWidth : tubeDisplay.glowHeight;
					float height = horiz ? tubeDisplay.glowHeight : tubeDisplay.glowWidth;

					submitScaled(CachedBufferer.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_CUBE, blockState)
						.light(0xf000f0)
						.disableDiffuse(), ms, queue, net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock(), width, height,  1);

					submitScaled(CachedBufferer
						.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_GLOW, blockState)
						.light(0xf000f0)
						.color(
							Math.min(((tubeDisplay.r & 0xFF) * 6 + 256) >> 3, 255),
							Math.min(((tubeDisplay.g & 0xFF) * 6 + 256) >> 3, 255),
							Math.min(((tubeDisplay.b & 0xFF) * 6 + 256) >> 3, 255),
							255)
						.disableDiffuse(), ms, queue, RenderTypes.additive(), width + 1.125f, height + 1.125f, 2);
				}

				submitScaled(CachedBufferer
					.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_BASE, blockState)
					.light(0xF000F0)
					.color(12, 12, 12, 255)
					.disableDiffuse(), ms, queue, RenderTypes.additive(), 1 + 1.25f / 16f);

				submitScaled(CachedBufferer
					.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE, blockState)
					.light(0xF000F0)
					.color(tubeDisplay.r, tubeDisplay.g, tubeDisplay.b, 255)
					.disableDiffuse(), ms, queue, RenderTypes.additive(), 1 + 1 / 16f);

				ms.popPose();
			}
		}

		ms.popPose();

	}

	/**
	 * Scaling moved off SuperByteBuffer onto the PoseStack it exposes, so scale-then-draw is wrapped
	 * here rather than repeated at every call site.
	 */
	private static void submitScaled(SuperByteBuffer buffer, PoseStack ms, SubmitNodeCollector queue,
		RenderType renderType, float x, float y, float z) {
		TransformStack.of(buffer.getTransforms())
			.scale(x, y, z);
		buffer.submit(ms, renderType, queue);
	}

	private static void submitScaled(SuperByteBuffer buffer, PoseStack ms, SubmitNodeCollector queue,
		RenderType renderType, float factor) {
		submitScaled(buffer, ms, queue, renderType, factor, factor, factor);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

}
