package com.simibubi.create.foundation.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * Arbitrary world geometry drawn inside a GUI.
 * <p>
 * Catnip covers the cases that are just a block, a block entity or a fluid; Create draws things that
 * are none of those - a blaze burner assembled from partial models, for one - and 26.2 only lets a
 * screen reach a {@link SubmitNodeCollector} through a picture-in-picture pass, so the drawing itself
 * travels with the render state.
 * <p>
 * The callback is invoked later in the same frame, so it must not read anything that changes in the
 * meantime; capture the values it needs when the state is built.
 */
public record GuiCustomGeometryRenderState(
	Geometry geometry,
	Matrix3x2f pose,
	GuiElementTransform transform,
	int x0, int y0,
	int x1, int y1,
	float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

	@FunctionalInterface
	public interface Geometry {
		void submit(PoseStack poseStack, SubmitNodeCollector collector);
	}

}
