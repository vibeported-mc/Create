package com.simibubi.create.gametest.client.gui;

import java.lang.reflect.Field;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/**
 * Driving Create's screens the way a player does, with the cursor.
 * <p>
 * The framework can click a button by its label, but only a vanilla one: Create's buttons and scroll
 * inputs all descend from Catnip's own widget rather than from {@code Button}, and are invisible to it.
 * So a widget is found on the screen, asked where it is, and then clicked at - which also means these
 * tests go through the same mouse handling a player's click does, and would notice if that broke.
 */
public final class ScreenTesting {

	/**
	 * How long to rest between actions, in milliseconds, from {@code -PguiBeat=500}.
	 * <p>
	 * Nothing by default, so a full run is not slowed down by pauses nobody is watching. Given a value,
	 * every click and scroll waits afterwards, which turns a run that is over before it can be followed
	 * into one that can be watched and checked by eye.
	 */
	private static final int BEAT_MILLIS = Integer.getInteger("create.gametest.gui.beatMillis", 0);

	/** Always at least a tick, since an action the game has not ticked on has not happened yet. */
	private static final int BEAT_TICKS = Math.max(1, Math.round(BEAT_MILLIS / 50f));

	private ScreenTesting() {
	}

	/** Where a widget sits, in the coordinates the screen itself is laid out in. */
	public record Bounds(int x, int y, int width, int height) {
		int middleX() {
			return x + width / 2;
		}

		int middleY() {
			return y + height / 2;
		}
	}

	public static <S extends Screen> S waitForScreen(ClientGameTestContext context, Class<S> type) {
		context.waitFor(client -> type.isInstance(client.gui.screen()));
		return context.computeOnClient(client -> type.cast(client.gui.screen()));
	}

	public static void waitForNoScreen(ClientGameTestContext context) {
		context.waitFor(client -> client.gui.screen() == null);
	}

	/**
	 * Where the widget the screen keeps in this field is.
	 * <p>
	 * By the name the screen calls it rather than by counting through the widgets it added, so that a
	 * test says which control it means and goes on meaning it when the screen gains another.
	 */
	public static Bounds widget(ClientGameTestContext context, String fieldName) {
		return context.computeOnClient(client -> boundsOf(read(client.gui.screen(), fieldName), fieldName));
	}

	/** The same, for one of a list of widgets - a row of buttons, or the brush parameters. */
	public static Bounds widget(ClientGameTestContext context, String fieldName, int index) {
		return context.computeOnClient(client -> {
			Object held = read(client.gui.screen(), fieldName);

			if (!(held instanceof List<?> widgets))
				throw new AssertionError(fieldName + " is not a list of widgets but a " + held.getClass());

			if (index >= widgets.size())
				throw new AssertionError(
					fieldName + " holds only " + widgets.size() + " widgets, so there is no " + index);

			return boundsOf(widgets.get(index), fieldName + "[" + index + "]");
		});
	}

	public static void click(ClientGameTestContext context, Bounds widget) {
		hover(context, widget);
		context.getInput()
			.pressMouse(0);
		context.waitTicks(BEAT_TICKS);
	}

	/**
	 * Turns the wheel over a widget, a notch at a time, since a scroll input moves one step to the notch.
	 * <p>
	 * Positive is the wheel turned up, as a player would turn it. Which way that moves a given control
	 * is the control's own business - a list of options runs the other way to a number, so that in both
	 * cases turning the wheel down goes down.
	 */
	public static void scroll(ClientGameTestContext context, Bounds widget, int notches) {
		hover(context, widget);

		for (int notch = 0; notch < Math.abs(notches); notch++) {
			context.getInput()
				.scroll(Math.signum(notches));
			context.waitTicks(BEAT_TICKS);
		}
	}

	/**
	 * Clicks a point of the screen rather than a widget, for the parts of a screen that are drawn and
	 * hit-tested by hand instead of being widgets at all - the schedule's card list among them.
	 */
	public static void clickAt(ClientGameTestContext context, int x, int y) {
		context.getInput()
			.setCursorPos(x * scale(context), y * scale(context));
		context.waitTicks(BEAT_TICKS);
		context.getInput()
			.pressMouse(0);
		context.waitTicks(BEAT_TICKS);
	}

	/** The value of one of the screen's own numbers, such as where it decided to put itself. */
	public static int number(ClientGameTestContext context, String fieldName) {
		return context.computeOnClient(client -> (Integer) read(client.gui.screen(), fieldName));
	}

	public static void hover(ClientGameTestContext context, Bounds widget) {
		double scale = scale(context);
		context.getInput()
			.setCursorPos(widget.middleX() * scale, widget.middleY() * scale);
		context.waitTicks(BEAT_TICKS);
	}

	/**
	 * How many window pixels the screen draws each of its own to.
	 * <p>
	 * The cursor is set in window pixels while a screen is laid out in its own, larger units, so one
	 * has to be turned into the other before aiming at anything.
	 */
	private static double scale(ClientGameTestContext context) {
		return context.computeOnClient(client -> (double) client.getWindow()
			.getScreenWidth()
			/ client.getWindow()
				.getGuiScaledWidth());
	}

	private static Bounds boundsOf(Object widget, String named) {
		if (!(widget instanceof AbstractWidget found))
			throw new AssertionError(named + " is not a widget but a " + widget.getClass());

		return new Bounds(found.getX(), found.getY(), found.getWidth(), found.getHeight());
	}

	/** The value of a field of the screen, or of anything it inherits from. */
	static Object read(Object target, String fieldName) {
		if (target == null)
			throw new AssertionError("No screen is open to look for " + fieldName + " on");

		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				Object value = field.get(target);

				if (value == null)
					throw new AssertionError(fieldName + " on " + target.getClass()
						.getSimpleName() + " is not set");

				return value;
			} catch (NoSuchFieldException lookHigher) {
				// Declared further up, if anywhere.
			} catch (IllegalAccessException cannotRead) {
				throw new AssertionError("Could not read " + fieldName, cannotRead);
			}
		}

		throw new AssertionError("No field " + fieldName + " on " + target.getClass());
	}

}
