package com.simibubi.create.foundation.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A set of widgets that are offset on the Z axis, allowing them to render above/below other "layers".
 */
public class ScreenOverlay extends CompositeWidget {
	public final int zOffset;

	public ScreenOverlay(int zOffset) {
		this.zOffset = zOffset;
	}

	// The GUI pose is two-dimensional in 26.2, so a layer cannot be pushed back along Z any more; draw
	// order is what decides what sits on top, and the widgets are already collected in order.
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}
}
