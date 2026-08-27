package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.mixin.accessor.FontAccessor;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

/**
 * The game's font, with the text shadow suppressed.
 * <p>
 * Minecraft 26.2 funnelled every way of drawing text through {@link Font#prepareText}, so this only
 * has to intercept that rather than the several drawInBatch overloads it used to.
 */
public class NoShadowFontWrapper extends Font {

	public NoShadowFontWrapper(Font wrapped) {
		super(((FontAccessor) wrapped).create$getProvider());
	}

	@Override
	public Font.PreparedText prepareText(String text, float x, float y, int originalColor, boolean drawShadow,
		int backgroundColor) {
		return super.prepareText(text, x, y, originalColor, false, backgroundColor);
	}

	@Override
	public Font.PreparedText prepareText(FormattedCharSequence text, float x, float y, int originalColor,
		boolean drawShadow, boolean includeEmpty, int backgroundColor) {
		return super.prepareText(text, x, y, originalColor, false, includeEmpty, backgroundColor);
	}

}
