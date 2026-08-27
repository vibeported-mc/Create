package com.simibubi.create.foundation.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.CatnipRenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;

/**
 * The pointer the radial wrench menu draws towards the sector under the cursor.
 * <p>
 * It used to be a triangle fan pushed straight through a {@code Tesselator}; 26.2 draws the GUI from
 * collected render states, so the fan is one of those now.
 */
public record RadialIndicatorRenderState(Matrix3x2f pose, int color) implements GuiElementRenderState {

	private static final float[][] RIM = { { 5, -5 }, { 3, -4.5F }, { 0, -4.2F }, { -3, -4.5F }, { -5, -5 } };

	@Override
	public RenderPipeline pipeline() {
		return CatnipRenderPipelines.TRIANGLE_FAN;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, 0, 0)
			.setColor(ARGB.color(191, color));
		for (float[] point : RIM)
			consumer.addVertexWith2DPose(pose, point[0], point[1])
				.setColor(ARGB.color(102, color));
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}

	@Override
	public ScreenRectangle bounds() {
		return new ScreenRectangle(-6, -6, 12, 12).transformMaxBounds(pose);
	}

}
