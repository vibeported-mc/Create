package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.filter.FilterScreen;
import com.simibubi.create.content.logistics.filter.PackageFilterScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The three filter screens, opened and worked the way a player opens and works them.
 * <p>
 * All three are a menu over the item in hand, and all three exist to write what was set onto that item
 * as they close, so each test ends by reading the item on the server. What the screen showed has to be
 * what the item was left with.
 * <p>
 * The plain filter is driven furthest, since it is the one with something to carry: an item is picked
 * up out of the player's own inventory and put into a filter slot with the cursor, the same two clicks
 * a player makes.
 */
@SharedWorld
public class FilterScreenTest {

	/** Long enough for the server to be asked for a menu and to answer. */
	private static final int SETTLE_TICKS = 10;

	/**
	 * Where the filter's own slots start.
	 * <p>
	 * A filter menu lays the player's own inventory out first - twenty seven and a hotbar of nine - and
	 * only then its own, so the first slot of the screen is one of the player's, not one of the filter's.
	 */
	private static final int FIRST_FILTER_SLOT = 36;

	@ClientGameTest(screenshot = false)
	@DisplayName("A filter keeps the item it was given and the list mode it was set to")
	void filterKeepsItsItemAndMode(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		openWith(context, singleplayer, server, "create:filter", FilterScreen.class);
		context.takeScreenshot(shot("filter_opened"));

		// Two clicks, as a player makes them: pick the cobblestone up out of the inventory, then put it
		// into the first filter slot. A filter slot only takes a likeness, so the cobblestone stays.
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, Items.COBBLESTONE)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_FILTER_SLOT));

		ScreenTesting.click(context, ScreenTesting.widget(context, "blacklist"));
		context.takeScreenshot(shot("filter_set"));

		close(context);

		assertTrue(heldHas(server, AllDataComponents.FILTER_ITEMS),
			"Closing the screen did not write the filter list onto the item");
		assertEquals(Items.COBBLESTONE, firstFiltered(server),
			"The item put into the filter slot is not the one the filter was left holding");
		assertEquals(Boolean.TRUE, held(server, AllDataComponents.FILTER_ITEMS_BLACKLIST),
			"Choosing the deny list did not reach the item");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("A filter set to ignore data says so on the item")
	void filterKeepsWhetherItRespectsData(ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		openWith(context, singleplayer, server, "create:filter", FilterScreen.class);

		// Something has to be in the list, since a filter with nothing in it is taken back off the item.
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, Items.COBBLESTONE)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_FILTER_SLOT));

		ScreenTesting.click(context, ScreenTesting.widget(context, "respectNBT"));
		close(context);

		assertEquals(Boolean.TRUE, held(server, AllDataComponents.FILTER_ITEMS_RESPECT_NBT),
			"Choosing to respect data did not reach the item");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("The attribute filter opens on its own item and closes onto it")
	void attributeFilterOpens(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		openWith(context, singleplayer, server, "create:attribute_filter", AttributeFilterScreen.class);
		context.takeScreenshot(shot("attribute_filter_opened"));

		// Its own toggle, between matching any attribute and all of them.
		ScreenTesting.click(context, ScreenTesting.widget(context, "blacklist"));
		close(context);

		assertNotNull(held(server, AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE),
			"Closing the attribute filter did not write its mode onto the item");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("The package filter takes an address and keeps it")
	void packageFilterKeepsItsAddress(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		openWith(context, singleplayer, server, "create:package_filter", PackageFilterScreen.class);

		context.getInput()
			.typeChars("depot");
		context.waitTicks(2);
		context.takeScreenshot(shot("package_filter_typed"));

		close(context);

		assertEquals("depot", held(server, AllDataComponents.PACKAGE_ADDRESS),
			"The address typed into the package filter did not reach the item");
	}

	/** The item in hand, right-clicked, which asks the server for the menu behind the screen. */
	private void openWith(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server, String item, Class<? extends Screen> screen) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with " + item);
		server.runCommand("item replace entity @a hotbar.1 with minecraft:cobblestone 1");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// A filter opens on a plain right-click, and does nothing at all on a sneaking one.
		ScreenTesting.rightClick(context);

		ScreenTesting.waitForScreen(context, screen);
	}

	/** Accepts the screen, which is what closes the menu and sends what was set to the server. */
	private void close(ClientGameTestContext context) {
		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);
	}

	/** What the item in the player's hand carries under this component, on the server. */
	private <T> T held(TestServerContext server, net.minecraft.core.component.DataComponentType<T> component) {
		return server.computeOnServer(minecraftServer -> heldItem(minecraftServer).get(component));
	}

	private boolean heldHas(TestServerContext server,
		net.minecraft.core.component.DataComponentType<?> component) {
		return server.computeOnServer(minecraftServer -> heldItem(minecraftServer).has(component));
	}

	/** The first item the filter was left holding. */
	private net.minecraft.world.item.Item firstFiltered(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var contents = heldItem(minecraftServer).get(AllDataComponents.FILTER_ITEMS);

			if (contents == null)
				return Items.AIR;

			for (var held : contents.nonEmptyItems())
				return held.typeHolder()
					.value();

			return Items.AIR;
		});
	}

	private static ItemStack heldItem(net.minecraft.server.MinecraftServer minecraftServer) {
		ServerPlayer player = minecraftServer.getPlayerList()
			.getPlayers()
			.get(0);

		return player.getMainHandItem();
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
