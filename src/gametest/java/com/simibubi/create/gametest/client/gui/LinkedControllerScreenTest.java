package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The linked controller's screen: twelve ghost slots, two to each of the six keys it can send on.
 * <p>
 * The frequencies are put in the way a player puts them in - an item picked up out of the player's own
 * inventory with the cursor and dropped onto a ghost slot - and the controller is then closed on its own
 * confirm button, which is what makes the menu write the slots back onto the item.
 * <p>
 * The trash button is worked as well, since clearing has its own path: it empties the slots on the client
 * and sends a packet, rather than going through the same save the confirm button does.
 */
@SharedWorld
public class LinkedControllerScreenTest {

	private static final int SETTLE_TICKS = 10;

	/**
	 * Where the controller's own slots start.
	 * <p>
	 * The menu lays the player's own inventory out first - twenty seven and a hotbar of nine - and only
	 * then its twelve, so the first slot of the screen is one of the player's.
	 */
	private static final int FIRST_FREQUENCY_SLOT = 36;

	@ClientGameTest(screenshot = false)
	@DisplayName("A frequency dropped into the linked controller is on the item once it is closed")
	void keepsTheFrequencyItWasGiven(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		open(context, singleplayer, server);
		context.takeScreenshot(shot("linked_controller_opened"));

		putIntoFirstSlot(context);
		context.takeScreenshot(shot("linked_controller_bound"));

		assertEquals(Items.COBBLESTONE, ScreenTesting.itemInSlot(context, FIRST_FREQUENCY_SLOT),
			"The frequency slot did not take what was dropped onto it");

		close(context, "confirmButton");

		assertEquals(List.of(Items.COBBLESTONE), frequencies(server),
			"The frequency put into the controller is not the one it was left holding");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("The linked controller's trash button clears the frequencies it was holding")
	void clearsTheFrequencies(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		open(context, singleplayer, server);
		putIntoFirstSlot(context);

		// Not the same path as confirming: clearing empties the slots and sends a packet of its own.
		ScreenTesting.click(context, ScreenTesting.widget(context, "resetButton"));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("linked_controller_cleared"));

		close(context, "confirmButton");

		assertTrue(frequencies(server).isEmpty(),
			"The trash button did not clear the controller, which still holds " + frequencies(server));
	}

	/** The gesture that opens it: sneaking and right-clicking, with the controller in the main hand. */
	private void open(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with create:linked_controller");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:cobblestone 1");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.sneakRightClick(context);

		ScreenTesting.waitForScreen(context, LinkedControllerScreen.class);
	}

	/**
	 * Two clicks, as a player makes them: the cobblestone up out of the inventory and down onto the first
	 * frequency slot. A ghost slot only takes a likeness, so the cobblestone itself stays where it was.
	 */
	private void putIntoFirstSlot(ClientGameTestContext context) {
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, Items.COBBLESTONE)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_FREQUENCY_SLOT));
	}

	private void close(ClientGameTestContext context, String button) {
		ScreenTesting.click(context, ScreenTesting.widget(context, button));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);
	}

	/** What the controller in the player's hand is bound to, on the server. */
	private List<Item> frequencies(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);
			ItemContainerContents bound = player.getMainHandItem()
				.get(AllDataComponents.LINKED_CONTROLLER_ITEMS);

			if (bound == null)
				return List.of();

			List<Item> found = new ArrayList<>();

			for (var frequency : bound.nonEmptyItems())
				found.add(frequency.typeHolder()
					.value());

			return found;
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
