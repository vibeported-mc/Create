package com.simibubi.create.content.equipment.zapper.terrainzapper;

import com.simibubi.create.foundation.gui.AllIcons;

/**
 * The icons the worldshaper's buttons are drawn with.
 * <p>
 * {@link TerrainTools} and {@link PlacementOptions} travel over the network and are loaded on a
 * dedicated server; {@link AllIcons} is a GUI class it cannot load. The mapping is unchanged, it
 * just no longer sits on the enum constants.
 */
public class TerrainZapperIcons {

	public static AllIcons of(TerrainTools tool) {
		return switch (tool) {
			case Fill -> AllIcons.I_FILL;
			case Place -> AllIcons.I_PLACE;
			case Replace -> AllIcons.I_REPLACE;
			case Clear -> AllIcons.I_CLEAR;
			case Overlay -> AllIcons.I_OVERLAY;
			case Flatten -> AllIcons.I_FLATTEN;
		};
	}

	public static AllIcons of(PlacementOptions option) {
		return switch (option) {
			case Merged -> AllIcons.I_CENTERED;
			case Attached -> AllIcons.I_ATTACHED;
			case Inserted -> AllIcons.I_INSERTED;
		};
	}

	private TerrainZapperIcons() {}
}
