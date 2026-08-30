package com.simibubi.create.gametest.client.trains;

import com.simibubi.create.gametest.client.gui.ScreenTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.MethodSource;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTests;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The train schedule screen, opened and worked the way a player opens and works it.
 * <p>
 * A right-click on a held schedule, and then the controls clicked where they are on screen. Unlike the
 * worldshaper this one is a menu, so the screen only arrives after the server has been asked for it,
 * and what it edits only reaches the item once the container is closed again.
 * <p>
 * The schedule's card list is drawn and hit-tested by hand rather than built out of widgets, so adding
 * an entry means clicking the place the screen looks for a click, not a button.
 */
@SharedWorld
public class ScheduleScreenTest {

	/** Long enough for the server to be asked for a menu and to answer. */
	private static final int SETTLE_TICKS = 10;

	/**
	 * Where the screen watches for a click on "add entry", in its own coordinates: the card list starts
	 * 25 in from each corner of the window, and the button sits at the top left of it.
	 */
	private static final int ADD_ENTRY_X = 25 + 26;
	private static final int ADD_ENTRY_Y = 25 + 7;

	/**
	 * Where the first condition of the first entry sits, in the same coordinates.
	 * <p>
	 * The screen takes a click in the card list, moves it into the card, and then moves it again into
	 * the condition area by 26 across and 28 down before dividing by the row height. So this is a point
	 * inside the first row of the first column of the first card.
	 */
	private static final int FIRST_CONDITION_X = 25 + 46;
	private static final int FIRST_CONDITION_Y = 25 + 37;

	@ClientGameTest(screenshot = false)
	@DisplayName("Looping is turned off, saved, read back, and turned on again")
	void carriesTheLoopSettingBothWays(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		giveSchedule(context, singleplayer, server);
		rightClickToOpen(context);
		context.takeScreenshot(shot("schedule_opened"));

		// A schedule with nothing in it is not a schedule: the server takes an empty one back off the
		// item rather than storing it. So there has to be somewhere to go before looping means anything.
		addDestinationEntry(context);

		// A schedule loops to begin with, so the first click turns looping off.
		boolean loopedToBeginWith = shownSchedule(context).cyclic;
		ScreenTesting.click(context, ScreenTesting.widget(context, "cyclicButton"));
		boolean loopedAfterFirstClick = shownSchedule(context).cyclic;
		context.takeScreenshot(shot("schedule_loop_turned_off"));

		assertNotEquals(loopedToBeginWith, loopedAfterFirstClick,
			"The first click on the loop button did not change what the screen shows");

		close(context);

		Schedule afterFirstClose = savedSchedule(server);
		assertNotNull(afterFirstClose, "Closing the screen did not write a schedule onto the item");
		assertEquals(1, afterFirstClose.entries.size(), "The entry the screen showed did not reach the item");
		assertEquals(loopedAfterFirstClick, afterFirstClose.cyclic,
			"Turning looping off was not what the item was left with");

		// Opening it again is the other half of the round trip: what was saved has to come back up.
		rightClickToOpen(context);
		context.takeScreenshot(shot("schedule_reopened"));

		assertEquals(afterFirstClose.cyclic, shownSchedule(context).cyclic,
			"Reopening the schedule did not show the loop setting that was saved on it");
		assertEquals(1, shownSchedule(context).entries.size(),
			"Reopening the schedule did not show the entry that was saved on it");

		ScreenTesting.click(context, ScreenTesting.widget(context, "cyclicButton"));
		boolean loopedAfterSecondClick = shownSchedule(context).cyclic;
		context.takeScreenshot(shot("schedule_loop_turned_on"));

		close(context);

		Schedule afterSecondClose = savedSchedule(server);
		assertNotNull(afterSecondClose, "Closing the screen again did not write a schedule onto the item");
		assertEquals(loopedAfterSecondClick, afterSecondClose.cyclic,
			"Turning looping back on was not what the item was left with");
		assertNotEquals(afterFirstClose.cyclic, afterSecondClose.cyclic,
			"The loop setting on the item came out the same both times round");
	}

	/** Every kind of instruction the editor offers, named by what it is called in the schedule. */
	static List<String> instructionTypes() {
		return Schedule.INSTRUCTION_TYPES.stream()
			.map(type -> type.getFirst()
				.toString())
			.toList();
	}

	@ClientGameTests(screenshot = false)
	@MethodSource("instructionTypes")
	@DisplayName("Every instruction the editor offers can be picked and lands on the schedule")
	void picksEveryInstruction(String id, ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		int wanted = instructionTypes().indexOf(id);

		giveSchedule(context, singleplayer, server);
		rightClickToOpen(context);

		ScreenTesting.clickAt(context, ScreenTesting.number(context, "leftPos") + ADD_ENTRY_X,
			ScreenTesting.number(context, "topPos") + ADD_ENTRY_Y);
		context.waitTicks(SETTLE_TICKS);

		// The editor opens on the first instruction, and the list runs the other way to the wheel, so
		// reaching the one wanted means turning the wheel down that many times.
		if (wanted > 0)
			ScreenTesting.scroll(context, ScreenTesting.widget(context, "scrollInput"), -wanted);

		context.takeScreenshot(shot("schedule_instruction_" + id.replace(':', '_')));

		ScreenTesting.click(context, ScreenTesting.widget(context, "editorConfirm"));
		context.waitTicks(SETTLE_TICKS);

		List<ScheduleEntry> entries = shownSchedule(context).entries;
		assertEquals(1, entries.size(), "Confirming the editor did not add an entry to the schedule");
		assertEquals(id, entries.get(0).instruction.getId()
			.toString(), "The editor did not leave the entry on the instruction that was picked");

		close(context);

		Schedule saved = savedSchedule(server);
		assertNotNull(saved, "Closing the screen did not write a schedule onto the item");
		assertEquals(id, saved.entries.get(0).instruction.getId()
			.toString(), "The instruction the screen showed did not reach the item");
	}

	/** Every kind of wait condition the editor offers. */
	static List<String> conditionTypes() {
		return Schedule.CONDITION_TYPES.stream()
			.map(type -> type.getFirst()
				.toString())
			.toList();
	}

	@ClientGameTests(screenshot = false)
	@MethodSource("conditionTypes")
	@DisplayName("Every wait condition the editor offers can be picked and lands on the schedule")
	void picksEveryCondition(String id, ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		int wanted = conditionTypes().indexOf(id);

		giveSchedule(context, singleplayer, server);
		rightClickToOpen(context);

		// A new entry comes with one wait condition already on it, which is the one to edit.
		addDestinationEntry(context);

		ScreenTesting.clickAt(context, ScreenTesting.number(context, "leftPos") + FIRST_CONDITION_X,
			ScreenTesting.number(context, "topPos") + FIRST_CONDITION_Y);
		context.waitTicks(SETTLE_TICKS);

		if (wanted > 0)
			ScreenTesting.scroll(context, ScreenTesting.widget(context, "scrollInput"), -wanted);

		context.takeScreenshot(shot("schedule_condition_" + id.replace(':', '_')));

		ScreenTesting.click(context, ScreenTesting.widget(context, "editorConfirm"));
		context.waitTicks(SETTLE_TICKS);

		assertEquals(id, firstCondition(shownSchedule(context)),
			"The editor did not leave the entry on the condition that was picked");

		close(context);

		Schedule saved = savedSchedule(server);
		assertNotNull(saved, "Closing the screen did not write a schedule onto the item");
		assertEquals(id, firstCondition(saved), "The condition the screen showed did not reach the item");
	}

	/** What the first wait condition of the first entry is. */
	private String firstCondition(Schedule schedule) {
		return schedule.entries.get(0).conditions.get(0)
			.get(0)
			.getId()
			.toString();
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("A destination can be added to a schedule and is written onto the item")
	void addsAnEntry(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		giveSchedule(context, singleplayer, server);
		rightClickToOpen(context);

		context.takeScreenshot(shot("schedule_before_entry"));
		addDestinationEntry(context);
		context.takeScreenshot(shot("schedule_entry_added"));

		assertEquals(1, shownSchedule(context).entries.size(),
			"Confirming the editor did not add an entry to the schedule");

		close(context);

		Schedule saved = savedSchedule(server);
		assertNotNull(saved, "Closing the screen did not write a schedule onto the item");
		assertEquals(1, saved.entries.size(), "The entry the screen showed did not reach the item");
	}

	/** An empty schedule in the player's hand, ready to be right-clicked. */
	private void giveSchedule(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with create:schedule");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * Right-clicks the held schedule, which asks the server for the menu behind the screen.
	 * <p>
	 * No sneaking for this one: a schedule opens on a plain right-click and does nothing on a sneaking
	 * one.
	 */
	private void rightClickToOpen(ClientGameTestContext context) {
		context.getInput()
			.pressMouse(1);

		ScreenTesting.waitForScreen(context, ScheduleScreen.class);
	}

	/** Accepts the screen, which closes the container and is what sends the schedule to the server. */
	private void close(ClientGameTestContext context) {
		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * Adds one destination to the open schedule, the way the screen offers it: a click on the spot it
	 * watches for one opens an editor on a new destination, and accepting that editor makes it an entry.
	 * <p>
	 * The card list is drawn and hit-tested by hand rather than built from widgets, so this is a click
	 * at a place rather than a click on a button.
	 */
	private void addDestinationEntry(ClientGameTestContext context) {
		ScreenTesting.clickAt(context, ScreenTesting.number(context, "leftPos") + ADD_ENTRY_X,
			ScreenTesting.number(context, "topPos") + ADD_ENTRY_Y);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("schedule_editing_entry"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "editorConfirm"));
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * The schedule the screen that is open right now is editing.
	 * <p>
	 * Asked of whatever screen is open rather than of one remembered from earlier, since taking a
	 * picture resizes the window and a screen laid out again is not always the same object.
	 */
	private Schedule shownSchedule(ClientGameTestContext context) {
		return context.computeOnClient(
			client -> (Schedule) ScreenTesting.read(client.gui.screen(), "schedule"));
	}

	/** The schedule written onto the held item, read on the server where the item really lives. */
	private Schedule savedSchedule(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);
			ItemStack held = player.getMainHandItem();
			CompoundTag tag = held.get(AllDataComponents.TRAIN_SCHEDULE);

			return tag == null ? null : Schedule.fromTag(player.registryAccess(), tag);
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
