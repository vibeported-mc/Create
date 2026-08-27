package com.simibubi.create.content.trains.display;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import org.joml.Matrix4f;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class FlapDisplayRenderer
	extends KineticBlockEntityRenderer<FlapDisplayBlockEntity, FlapDisplayRenderer.FlapDisplayRenderState> {

	private static final RenderType GLYPH_RENDER_TYPE = Minecraft.getInstance()
		.font.getFontSet(Style.DEFAULT_FONT)
		.whiteGlyph()
		.renderType(Font.DisplayMode.NORMAL);

	public static class FlapDisplayRenderState extends KineticRenderState {
		public final List<LineSnapshot> lines = new ArrayList<>();
		public int xSize = 1;
		public float yRot;
		public boolean paused;
		public @Nullable Level level;
	}

	public record LineSnapshot(List<SectionSnapshot> sections, int color, boolean glowing) {
	}

	public record SectionSnapshot(FlapDisplaySection section, String text) {
	}


	public FlapDisplayRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public FlapDisplayRenderState createRenderState() {
		return new FlapDisplayRenderState();
	}

	@Override
	protected void extractSafe(FlapDisplayBlockEntity be, FlapDisplayRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.lines.clear();

		if (!be.isController)
			return;

		state.xSize = be.xSize;
		state.yRot = AngleHelper.horizontalAngle(be.getBlockState()
			.getValue(FlapDisplayBlock.HORIZONTAL_FACING));
		state.paused = !be.isSpeedRequirementFulfilled();
		state.level = be.getLevel();

		int ticks = AnimationTickHolder.getTicks(be.getLevel());
		List<FlapDisplayLayout> lines = be.getLines();

		for (int j = 0; j < lines.size(); j++) {
			List<FlapDisplaySection> line = lines.get(j)
				.getSections();
			List<SectionSnapshot> sections = new ArrayList<>(line.size());

			for (int i = 0; i < line.size(); i++) {
				FlapDisplaySection section = line.get(i);
				// The section keeps mutating on the block entity, so the few values the glyph pass
				// reads are copied out here.
				String text = section.renderCharsIndividually() || !section.spinning[0] ? section.text
					: section.cyclingOptions[((ticks / 3) + i * 13) % section.cyclingOptions.length];
				sections.add(new SectionSnapshot(section, text));
			}

			state.lines.add(new LineSnapshot(sections, be.getLineColor(j), be.isLineGlowing(j)));
		}
	}

	@Override
	protected void submitSafe(FlapDisplayRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		if (state.lines.isEmpty())
			return;

		float scale = 1 / 32f;

		ms.pushPose();
		TransformStack.of(ms)
			.center()
			.rotateYDegrees(state.yRot)
			.uncenter()
			.translate(0, 0, -3 / 16f);

		ms.translate(0, 1, 1);
		ms.scale(scale, scale, scale);
		ms.scale(1, -1, 1);
		ms.translate(0, 0, 1 / 2f);

		for (int j = 0; j < state.lines.size(); j++) {
			LineSnapshot line = state.lines.get(j);
			ms.pushPose();

			float w = 0;
			for (SectionSnapshot section : line.sections())
				w += section.section()
					.getSize() + (section.section().hasGap ? 8 : 1);
			ms.translate(state.xSize * 16 - w / 2 + 1, 4.5f, 0);

			// Glyphs go through a custom geometry node: the font's baked glyphs draw straight into a
			// vertex consumer, which the queue only hands out at draw time.
			PoseStack lineStack = new PoseStack();
			lineStack.last()
				.set(ms.last());
			List<SectionSnapshot> sections = line.sections();
			int color = line.color();
			boolean glowing = line.glowing();

			queue.submitCustomGeometry(ms, GLYPH_RENDER_TYPE, (pose, consumer) -> {
				PoseStack local = new PoseStack();
				local.last()
					.set(pose);
				FlapDisplayRenderOutput renderOutput = new FlapDisplayRenderOutput(consumer, color,
					local.last()
						.pose(),
					state.lightCoords, j, state.paused, state.level, glowing);

				for (SectionSnapshot section : sections) {
					renderOutput.nextSection(section.section());
					StringDecomposer.iterateFormatted(section.text(), Style.EMPTY, renderOutput);
					local.translate(section.section().size + (section.section().hasGap ? 8 : 1), 0, 0);
					renderOutput.pose.set(local.last()
						.pose());
				}
			});

			ms.popPose();
			ms.translate(0, 16, 0);
		}

		ms.popPose();
	}

	@OnlyIn(Dist.CLIENT)
	static class FlapDisplayRenderOutput implements FormattedCharSink {

		final VertexConsumer consumer;
		final float r, g, b, a;
		final Matrix4f pose;
		final int light;
		final boolean paused;

		FlapDisplaySection section;
		float x;
		private int lineIndex;
		private Level level;

		public FlapDisplayRenderOutput(VertexConsumer consumer, int color, Matrix4f pose, int light, int lineIndex,
			boolean paused, Level level, boolean glowing) {
			this.consumer = consumer;
			this.lineIndex = lineIndex;
			this.level = level;
			this.a = glowing ? .975f : .85f;
			this.r = (color >> 16 & 255) / 255f;
			this.g = (color >> 8 & 255) / 255f;
			this.b = (color & 255) / 255f;
			this.pose = pose;
			this.light = glowing ? 0xf000f0 : light;
			this.paused = paused;
		}

		public void nextSection(FlapDisplaySection section) {
			this.section = section;
			x = 0;
		}

		public boolean accept(int charIndex, Style style, int glyph) {
			FontSet fontset = getFontSet();
			int ticks = paused ? 0 : AnimationTickHolder.getTicks(level);
			float time = paused ? 0 : AnimationTickHolder.getRenderTime();
			float dim = 1;

			if (section.renderCharsIndividually() && section.spinning[Math.min(charIndex, section.spinning.length)]) {
				float speed = section.spinningTicks > 5 && section.spinningTicks < 20 ? 1.75f : 2.5f;
				float cycle = (time / speed) + charIndex * 16.83f + lineIndex * 0.75f;
				float partial = cycle % 1;
				char cyclingGlyph = section.cyclingOptions[((int) cycle) % section.cyclingOptions.length].charAt(0);
				glyph = paused ? cyclingGlyph : partial > 1 / 2f ? partial > 3 / 4f ? '_' : '-' : cyclingGlyph;
				dim = 0.75f;
			}

			GlyphInfo glyphinfo = fontset.getGlyphInfo(glyph, false);
			float glyphWidth = glyphinfo.getAdvance(false);

			if (!section.renderCharsIndividually() && section.spinning[0]) {
				glyph = ticks % 3 == 0 ? glyphWidth == 6 ? '-' : glyphWidth == 1 ? '\'' : glyph : glyph;
				glyph = ticks % 3 == 2 ? glyphWidth == 6 ? '_' : glyphWidth == 1 ? '.' : glyph : glyph;
				if (ticks % 3 != 1)
					dim = 0.75f;
			}

			BakedGlyph bakedglyph =
				style.isObfuscated() && glyph != 32 ? fontset.getRandomGlyph(glyphinfo) : fontset.getGlyph(glyph);
			TextColor textcolor = style.getColor();

			float red = this.r * dim;
			float green = this.g * dim;
			float blue = this.b * dim;

			if (textcolor != null) {
				int i = textcolor.getValue();
				red = (i >> 16 & 255) / 255f;
				green = (i >> 8 & 255) / 255f;
				blue = (i & 255) / 255f;
			}

			float standardWidth = section.wideFlaps ? FlapDisplaySection.WIDE_MONOSPACE : FlapDisplaySection.MONOSPACE;

			if (section.renderCharsIndividually())
				x += (standardWidth - glyphWidth) / 2f;

			if (isNotEmpty(bakedglyph))
				bakedglyph.render(style.isItalic(), x, 0, pose, consumer, red, green, blue, a, light);

			if (section.renderCharsIndividually())
				x += standardWidth - (standardWidth - glyphWidth) / 2f;
			else
				x += glyphWidth;

			return true;
		}

		public float finish(int bgColor) {
			if (bgColor == 0)
				return x;

			float a = (bgColor >> 24 & 255) / 255f;
			float r = (bgColor >> 16 & 255) / 255f;
			float g = (bgColor >> 8 & 255) / 255f;
			float b = (bgColor & 255) / 255f;

			BakedGlyph bakedglyph = getFontSet().whiteGlyph();
			bakedglyph.renderEffect(new BakedGlyph.Effect(-1f, 9f, section.size, -2f, 0.01f, r, g, b, a), this.pose,
				consumer, light);

			return x;
		}

		private FontSet getFontSet() {
			return Minecraft.getInstance().font.getFontSet(Style.DEFAULT_FONT);
		}

		private boolean isNotEmpty(BakedGlyph bakedglyph) {
			return !(bakedglyph instanceof EmptyGlyph);
		}

	}

	@Override
	protected SuperByteBuffer getRotatedModel(FlapDisplayBlockEntity be, BlockState state) {
		return CachedBufferer.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state,
			state.getValue(FlapDisplayBlock.HORIZONTAL_FACING));
	}

	/**
	 * 26.2 dropped the block entity argument. Only controllers extract anything, so a non-controller
	 * costs a visibility check rather than geometry.
	 */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

}
