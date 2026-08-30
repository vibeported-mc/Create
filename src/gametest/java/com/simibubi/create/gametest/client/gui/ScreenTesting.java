package com.simibubi.create.gametest.client.gui;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

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

	/** Long enough for the client to have been moved, and to have worked out what it is looking at. */
	private static final int AIM_TICKS = 5;

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

	/**
	 * Closes the open screen with escape, for the screens that have no button of their own to accept them.
	 * <p>
	 * A screen that sends what it was set to as it is taken off the screen treats this as the ordinary way
	 * out, so it is not a discarding of anything.
	 */
	public static void closeWithEscape(ClientGameTestContext context) {
		context.getInput()
			.pressKey(GLFW.GLFW_KEY_ESCAPE);
		waitForNoScreen(context);
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

	/** One of a grid of widgets: a list of rows, each a list of controls, as the sequencer keeps them. */
	public static Bounds widget(ClientGameTestContext context, String fieldName, int row, int column) {
		return context.computeOnClient(
			client -> boundsOf(nested(client.gui.screen(), fieldName, row, column), fieldName));
	}

	/**
	 * What a scroll input is showing.
	 * <p>
	 * Held against what the block was left with, so that a test says the two agree rather than naming a
	 * number that depends on where the control started and how far a notch moves it.
	 */
	public static int scrollState(ClientGameTestContext context, String fieldName) {
		return context.computeOnClient(client -> ((ScrollInput) read(client.gui.screen(), fieldName)).getState());
	}

	public static int scrollState(ClientGameTestContext context, String fieldName, int row, int column) {
		return context.computeOnClient(
			client -> ((ScrollInput) nested(client.gui.screen(), fieldName, row, column)).getState());
	}

	private static Object nested(Screen screen, String fieldName, int row, int column) {
		Object held = read(screen, fieldName);

		if (!(held instanceof List<?> rows))
			throw new AssertionError(fieldName + " is not a list of rows but a " + held.getClass());

		if (!(rows.get(row) instanceof List<?> controls))
			throw new AssertionError(fieldName + " row " + row + " is not a list of widgets");

		return controls.get(column);
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

	/**
	 * The gesture that opens a held item's own screen: the sneak key held down, then a right-click.
	 * <p>
	 * The key has to be down for a tick or two first, since the screen only opens for a player the game
	 * already considers to be sneaking rather than one who has only just pressed the key.
	 */
	public static void sneakRightClick(ClientGameTestContext context) {
		context.getInput()
			.holdKey(options -> options.keyShift);
		context.waitTicks(2);

		context.getInput()
			.pressMouse(1);
		context.waitTicks(2);

		context.getInput()
			.releaseKey(options -> options.keyShift);
	}

	/** A plain right-click, for the items that open on one and do something else when sneaked at. */
	public static void rightClick(ClientGameTestContext context) {
		context.getInput()
			.pressMouse(1);
	}

	/**
	 * Puts the player in front of a block, looking at the middle of it.
	 * <p>
	 * Standing rather than spectating, since a spectator's clicks pass through the world, and a couple of
	 * blocks back so that the face being aimed at is well within reach.
	 */
	public static void lookAtBlock(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		lookAt(context, server, Vec3.atCenterOf(pos));
	}

	/**
	 * Puts the player in front of a particular spot, looking straight at it.
	 * <p>
	 * For the blocks that keep more than one thing in a single face - a factory gauge holds four panels in
	 * one - where aiming at the middle of the block lands on the corner between them.
	 */
	public static void lookAt(ClientGameTestContext context, TestServerContext server, Vec3 point) {
		lookAt(context, server, point, point.add(0, -1, 2.5));
	}

	/**
	 * The same, from a chosen place.
	 * <p>
	 * A face can only be clicked from the side it is on, so putting a block down on the ground means
	 * standing above it and looking down rather than standing in front of it.
	 */
	public static void lookAt(ClientGameTestContext context, TestServerContext server, Vec3 point, Vec3 from) {
		server.runOnServer(minecraftServer -> {
			ServerPlayer player = onlyPlayer(minecraftServer);
			player.setGameMode(GameType.CREATIVE);

			// Left hovering rather than stood on anything. Where a player is put need not be where the
			// ground is, and one who is falling is looking from somewhere they are about to leave - so
			// rather than wait out a fall of unknown length, there is no fall.
			player.getAbilities().flying = true;
			player.onUpdateAbilities();

			player.connection.teleport(from.x, from.y, from.z, 180, 0);
		});

		context.waitTicks(AIM_TICKS);

		server.runOnServer(minecraftServer -> {
			ServerPlayer player = onlyPlayer(minecraftServer);
			Vec3 toBlock = point.subtract(player.getEyePosition());

			float yaw = (float) (Mth.atan2(toBlock.z, toBlock.x) * 180 / Math.PI) - 90;
			float pitch = (float) -(Mth.atan2(toBlock.y, toBlock.horizontalDistance()) * 180 / Math.PI);

			player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
		});

		context.waitTicks(AIM_TICKS);
	}

	private static ServerPlayer onlyPlayer(MinecraftServer minecraftServer) {
		return minecraftServer.getPlayerList()
			.getPlayers()
			.get(0);
	}

	/**
	 * Right-clicks a block in the world, which is how most of Create's blocks open their screens.
	 * <p>
	 * Checks what the player ended up looking at first, so that a test which failed to reach the block at
	 * all says so, rather than failing later on a screen that was never opened.
	 */
	public static void rightClickBlock(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		rightClickAt(context, server, Vec3.atCenterOf(pos), pos);
	}

	/**
	 * Right-clicks a particular spot, checking that the block it belongs to is the one meant.
	 * <p>
	 * Aiming at a point on a face rather than at the middle of a block is what lets the same quarter of a
	 * gauge be hit twice running: once to hang the panel there and once to open it.
	 */
	public static void rightClickAt(ClientGameTestContext context, TestServerContext server, Vec3 point,
		BlockPos expected) {
		rightClickAt(context, server, point, point.add(0, -1, 2.5), expected);
	}

	/**
	 * Holds the right button down on a spot rather than clicking it.
	 * <p>
	 * Which is how a block's value board is opened: it appears only once the button has been held for a
	 * few ticks, and what is set is decided by where the cursor is when the button is let go again.
	 */
	public static void holdRightClickAt(ClientGameTestContext context, TestServerContext server, Vec3 point,
		BlockPos expected) {
		lookAt(context, server, point);
		requireLookingAt(context, server, point, expected);

		context.getInput()
			.holdMouse(1);
	}

	/** The same, from a chosen place, for the faces that cannot be seen from in front. */
	public static void rightClickAt(ClientGameTestContext context, TestServerContext server, Vec3 point,
		Vec3 from, BlockPos expected) {
		lookAt(context, server, point, from);

		requireLookingAt(context, server, point, expected);

		context.getInput()
			.pressMouse(1);
	}

	private static void requireLookingAt(ClientGameTestContext context, TestServerContext server, Vec3 point,
		BlockPos expected) {
		BlockPos looking = context.computeOnClient(
			client -> client.hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null);

		if (!expected.equals(looking))
			throw new AssertionError("The player was aimed at " + point + " but is looking at " + looking
				+ ", from " + context.computeOnClient(client -> client.player.position() + " pitch "
					+ client.player.getXRot() + " yaw " + client.player.getYRot())
				+ "; server says " + server.computeOnServer(minecraftServer -> minecraftServer.getPlayerList()
					.getPlayers()
					.get(0)
					.position()
					.toString()));
	}

	/**
	 * Right-clicks whatever is hanging in this space rather than the block itself.
	 * <p>
	 * A blueprint and its like are entities standing flush against a wall, so the crosshair reaches them
	 * before it reaches anything solid. Checking that it really is an entity under the crosshair keeps a
	 * missed click from turning into a click on the wall behind it.
	 */
	public static void rightClickEntityAt(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		lookAtBlock(context, server, pos);

		HitResult.Type looking = context.computeOnClient(
			client -> client.hitResult == null ? HitResult.Type.MISS : client.hitResult.getType());

		if (looking != HitResult.Type.ENTITY)
			throw new AssertionError(
				"The player was put in front of " + pos + " but is looking at " + looking + ", not an entity");

		context.getInput()
			.pressMouse(1);
	}

	/**
	 * Empties the ground around a spot and lays a floor under it.
	 * <p>
	 * A block screen is opened by a player standing in front of the block and clicking it, so there has
	 * to be somewhere to stand and nothing in the way - and the world these tests share holds whatever
	 * the test before left in it.
	 */
	public static void clearGround(TestServerContext server, BlockPos centre, int radius) {
		server.runCommand("fill %d %d %d %d %d %d air".formatted(centre.getX() - radius, centre.getY() - 1,
			centre.getZ() - radius, centre.getX() + radius, centre.getY() + radius, centre.getZ() + radius));
		server.runCommand("fill %d %d %d %d %d %d stone".formatted(centre.getX() - radius, centre.getY() - 1,
			centre.getZ() - radius, centre.getX() + radius, centre.getY() - 1, centre.getZ() + radius));
		server.runCommand("kill @e[type=item]");
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
	 * Where a slot of the open menu is.
	 * <p>
	 * A slot is not a widget: a menu screen keeps its own list and draws them itself, so they are found
	 * through the menu rather than among the screen's controls.
	 */
	public static Bounds slot(ClientGameTestContext context, int index) {
		return context.computeOnClient(client -> slotPosition(menuScreen(client.gui.screen()), index));
	}

	static Bounds slotPosition(AbstractContainerScreen<?> screen, int index) {
		Slot slot = screen.getMenu().slots.get(index);

		return new Bounds((Integer) read(screen, "leftPos") + slot.x, (Integer) read(screen, "topPos") + slot.y,
			16, 16);
	}

	/** What a slot of the open menu is showing, which for a ghost slot is what it was set to. */
	public static Item itemInSlot(ClientGameTestContext context, int index) {
		return context.computeOnClient(client -> menuScreen(client.gui.screen()).getMenu().slots.get(index)
			.getItem()
			.getItem());
	}

	/**
	 * The first slot of the open menu holding this item, so that a test can say which item it means to
	 * pick up rather than counting through a layout.
	 */
	public static int slotHolding(ClientGameTestContext context, Item item) {
		return context.computeOnClient(client -> {
			List<Slot> slots = menuScreen(client.gui.screen()).getMenu().slots;

			for (int index = 0; index < slots.size(); index++)
				if (slots.get(index)
					.getItem()
					.is(item))
					return index;

			throw new AssertionError("No slot on this screen is holding " + item);
		});
	}

	private static AbstractContainerScreen<?> menuScreen(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> menu))
			throw new AssertionError("The open screen is not a menu but a " + screen);

		return menu;
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

	/** Moves the cursor to a point of the screen without clicking, for a board that is dragged across. */
	public static void hoverAt(ClientGameTestContext context, double x, double y) {
		double scale = scale(context);
		context.getInput()
			.setCursorPos(x * scale, y * scale);
		context.waitTicks(BEAT_TICKS);
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

	/**
	 * The result of calling one of the target's own methods.
	 * <p>
	 * For the few things a test has to ask about that are declared on a class it cannot name - a
	 * blueprint's sections among them, which are an inner class kept to itself.
	 */
	static Object invoke(Object target, String methodName, Object... arguments) {
		if (target == null)
			throw new AssertionError("Nothing to call " + methodName + " on");

		Class<?>[] types = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; i++)
			types[i] = arguments[i].getClass();

		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Method method = type.getDeclaredMethod(methodName, types);
				method.setAccessible(true);

				return method.invoke(target, arguments);
			} catch (NoSuchMethodException lookHigher) {
				// Declared further up, if anywhere.
			} catch (IllegalAccessException | InvocationTargetException cannotCall) {
				throw new AssertionError("Could not call " + methodName, cannotCall);
			}
		}

		throw new AssertionError("No method " + methodName + " on " + target.getClass());
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
