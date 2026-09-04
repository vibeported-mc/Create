package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.foundation.gui.AllIcons;

/**
 * The icon a {@link PlacementPatterns} button is drawn with.
 * <p>
 * {@link AllIcons} is a GUI class, and naming one from the enum's own constants made a dedicated
 * server load it while registering Create's packets. The mapping is the same; only the side it lives
 * on has changed.
 */
public class PlacementPatternsClient {

	public static AllIcons iconFor(PlacementPatterns pattern) {
		return switch (pattern) {
			case Solid -> AllIcons.I_PATTERN_SOLID;
			case Checkered -> AllIcons.I_PATTERN_CHECKERED;
			case InverseCheckered -> AllIcons.I_PATTERN_CHECKERED_INVERSED;
			case Chance25 -> AllIcons.I_PATTERN_CHANCE_25;
			case Chance50 -> AllIcons.I_PATTERN_CHANCE_50;
			case Chance75 -> AllIcons.I_PATTERN_CHANCE_75;
		};
	}

	private PlacementPatternsClient() {}
}
