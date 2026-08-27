package com.simibubi.create.foundation.utility;

import net.minecraft.world.level.Level;

/**
 * What Create's clocks read now that a dimension keeps its own clock.
 * <p>
 * 26.2 replaced {@code Level#getDayTime} and {@code DimensionType#natural} with per-dimension world
 * clocks: a dimension may have one, or none at all. A dimension without a clock is what "not natural"
 * used to mean - the nether and the end - where Create's clocks spin nonsensically fast on purpose.
 */
public class WorldClocks {

	/**
	 * Whether this dimension runs a day cycle of its own.
	 */
	public static boolean hasDayCycle(Level level) {
		return level.dimensionType()
			.defaultClock()
			.isPresent();
	}

	/**
	 * The time of day a clock in this dimension shows, in ticks of a 24000-tick day.
	 */
	public static int getDayTime(Level level) {
		long time = hasDayCycle(level) ? level.getDefaultClockTime() : level.getOverworldClockTime() * 24;
		return (int) (time % 24000);
	}

	private WorldClocks() {
	}

}
