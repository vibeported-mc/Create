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
	float scale,
	/**
	 * Whether the callback composes its own parts.
	 *
	 * <p>A widget made of several parts has to draw them all into one pass, or they are composited as
	 * separate flat quads and cannot resolve against each other in depth. Such a callback wants only
	 * the anchor set up, and does the offset, flip and rotation itself for each part, so that every
	 * one of them gets 1.21.1's order rather than sharing a single flip on the wrong side of the
	 * offsets.
	 */
	boolean sceneSpace, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

	/**
	 * The rectangle this element covers on screen.
	 *
	 * <p>{@code GuiRenderState.findAppropriateNode} drops any element whose bounds are null, without
	 * a word, and every caller here passes null -- so none of this geometry was ever drawn. The same
	 * mistake is in Catnip's three states, and it is what emptied every Create machine out of its JEI
	 * recipe panel.
	 *
	 * <p>{@code x0..y1} are a local 0-16 box rather than screen coordinates, because they also size
	 * the offscreen texture; the placement is in {@link #pose()}. The rectangle is therefore that box
	 * put through the pose.
	 */
	@Override
	public ScreenRectangle bounds() {
		if (bounds != null) {
			return bounds;
		}

		ScreenRectangle onScreen = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		return scissorArea != null ? scissorArea.intersection(onScreen) : onScreen;
	}

	@FunctionalInterface
	public interface Geometry {
		void submit(PoseStack poseStack, SubmitNodeCollector collector);
	}

}
