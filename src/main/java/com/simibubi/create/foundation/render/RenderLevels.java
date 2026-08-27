package com.simibubi.create.foundation.render;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.Level;

/**
 * Where a contraption's lighting is sampled from.
 * <p>
 * {@code SuperByteBuffer#useLevelLight} transforms each vertex into world space before looking its
 * light up, so it has to be given the level the contraption is moving through - not the virtual world
 * holding its blocks. 26.2 narrowed that parameter to {@link BlockAndTintGetter}, which a
 * {@link Level} no longer is; on the client the level always is one, and the virtual world stands in
 * for the rare case where it is not.
 */
public class RenderLevels {

	public static BlockAndTintGetter lightSource(Level level, BlockAndTintGetter fallback) {
		return level instanceof BlockAndTintGetter tintGetter ? tintGetter : fallback;
	}

	private RenderLevels() {
	}

}
