package com.simibubi.create.content.trains.display;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlapDisplayRenderer
	extends KineticBlockEntityRenderer<FlapDisplayBlockEntity, FlapDisplayRenderer.FlapDisplayRenderState> {

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

	private final Font font;

	public FlapDisplayRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		this.font = context.font();
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

		int ticks = AnimationTickHolder.getTicks();
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

			// A glyph is a drawable of its own in 26.2, and which one it belongs to decides its render
			// type, so the line's glyphs are gathered per type and each group submitted as one node.
			FlapDisplayRenderOutput renderOutput =
				new FlapDisplayRenderOutput(font, line.color(), j, state.paused, line.glowing());

			float offset = 0;
			for (SectionSnapshot section : line.sections()) {
				renderOutput.nextSection(section.section(), offset);
				StringDecomposer.iterateFormatted(section.text(), Style.EMPTY, renderOutput);
				offset += section.section().size + (section.section().hasGap ? 8 : 1);
			}

			int light = state.lightCoords;
			renderOutput.glyphs.forEach((type, glyphs) -> queue.submitCustomGeometry(ms, type, (pose, consumer) -> {
				for (TextRenderable glyph : glyphs)
					glyph.render(pose.pose(), consumer, light, true);
			}));

			ms.popPose();
			ms.translate(0, 16, 0);
		}

		ms.popPose();
	}

	/**
	 * Turns a line's characters into glyph drawables, grouped by the render type each one needs.
	 */
	static class FlapDisplayRenderOutput implements FormattedCharSink {

		final Map<RenderType, List<TextRenderable>> glyphs = new IdentityHashMap<>();
		final Font font;
		final int color;
		final int alpha;
		final boolean paused;

		FlapDisplaySection section;
		float x;
		private final int lineIndex;

		public FlapDisplayRenderOutput(Font font, int color, int lineIndex, boolean paused, boolean glowing) {
			this.font = font;
			this.lineIndex = lineIndex;
			this.color = color & 0x00_FFFFFF;
			this.alpha = (glowing ? 249 : 217) << 24;
			this.paused = paused;
		}

		public void nextSection(FlapDisplaySection section, float offset) {
			this.section = section;
			x = offset;
		}

		public boolean accept(int charIndex, Style style, int glyph) {
			GlyphSource glyphSource = font.getGlyphSource(style.getFont());
			int ticks = paused ? 0 : AnimationTickHolder.getTicks();
			float time = paused ? 0 : AnimationTickHolder.getRenderTime();
			boolean dim = false;

			if (section.renderCharsIndividually() && section.spinning[Math.min(charIndex, section.spinning.length)]) {
				float speed = section.spinningTicks > 5 && section.spinningTicks < 20 ? 1.75f : 2.5f;
				float cycle = (time / speed) + charIndex * 16.83f + lineIndex * 0.75f;
				float partial = cycle % 1;
				char cyclingGlyph = section.cyclingOptions[((int) cycle) % section.cyclingOptions.length].charAt(0);
				glyph = paused ? cyclingGlyph : partial > 1 / 2f ? partial > 3 / 4f ? '_' : '-' : cyclingGlyph;
				dim = true;
			}

			BakedGlyph bakedglyph = glyphSource.getGlyph(glyph);
			float glyphWidth = bakedglyph.info()
				.getAdvance(false);

			if (!section.renderCharsIndividually() && section.spinning[0]) {
				int replacement = glyph;
				replacement = ticks % 3 == 0 ? glyphWidth == 6 ? '-' : glyphWidth == 1 ? '\'' : replacement : replacement;
				replacement = ticks % 3 == 2 ? glyphWidth == 6 ? '_' : glyphWidth == 1 ? '.' : replacement : replacement;
				if (replacement != glyph) {
					glyph = replacement;
					bakedglyph = glyphSource.getGlyph(glyph);
				}
				if (ticks % 3 != 1)
					dim = true;
			}

			if (style.isObfuscated() && glyph != 32)
				bakedglyph = glyphSource.getRandomGlyph(font.random, Mth.ceil(glyphWidth));

			TextColor textcolor = style.getColor();
			int drawColor;
			if (textcolor != null)
				drawColor = alpha | textcolor.getValue();
			else if (dim)
				drawColor = alpha | dim(color);
			else
				drawColor = alpha | color;

			float standardWidth = section.wideFlaps ? FlapDisplaySection.WIDE_MONOSPACE : FlapDisplaySection.MONOSPACE;

			if (section.renderCharsIndividually())
				x += (standardWidth - glyphWidth) / 2f;

			TextRenderable drawable = bakedglyph.createGlyph(x, 0, drawColor, 0, style, 0, 0);
			if (drawable != null)
				glyphs.computeIfAbsent(drawable.renderType(Font.DisplayMode.NORMAL), t -> new ArrayList<>())
					.add(drawable);

			if (section.renderCharsIndividually())
				x += standardWidth - (standardWidth - glyphWidth) / 2f;
			else
				x += glyphWidth;

			return true;
		}

		/** Three quarters of each channel, the way the old float channels were scaled. */
		private static int dim(int rgb) {
			return (rgb >> 16 & 255) * 192 / 256 << 16 | (rgb >> 8 & 255) * 192 / 256 << 8 | (rgb & 255) * 192 / 256;
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
