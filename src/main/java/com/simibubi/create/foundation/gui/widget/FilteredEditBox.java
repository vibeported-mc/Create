package com.simibubi.create.foundation.gui.widget;

import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * An edit box that refuses text its owner does not accept.
 * <p>
 * Minecraft 26.2 dropped {@code EditBox#setFilter} with nothing in its place. The one hook left,
 * {@code setResponder}, reports a change after the fact and is already spoken for elsewhere, so the
 * check goes where the text actually enters: everything typed, pasted or deleted lands in
 * {@link #insertText}, and the box simply declines an edit that would leave it holding a value the
 * filter rejects.
 */
public class FilteredEditBox extends EditBox {

	private Predicate<String> filter = s -> true;

	public FilteredEditBox(Font font, int x, int y, int width, int height, Component message) {
		super(font, x, y, width, height, message);
	}

	public void setFilter(Predicate<String> filter) {
		this.filter = filter;
	}

	@Override
	public void insertText(String input) {
		String before = getValue();
		int cursorBefore = getCursorPosition();
		super.insertText(input);
		if (filter.test(getValue()))
			return;

		// Put back exactly what was there, cursor included, so a rejected keystroke does nothing.
		setValue(before);
		moveCursorTo(cursorBefore, false);
	}

}
