package com.simibubi.create.foundation.gui.widget;

import java.util.List;

import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.minecraft.network.chat.Component;

public class TooltipArea extends AbstractSimiWidget {

	public TooltipArea(int x, int y, int width, int height) {
		super(x, y, width, height);
	}

	// Nothing is drawn: the area exists only to show a tooltip, and hover and tooltip are both the
	// base widget's business in 26.2.

	public TooltipArea withTooltip(List<Component> tooltip) {
		this.toolTip = tooltip;
		return this;
	}

}
