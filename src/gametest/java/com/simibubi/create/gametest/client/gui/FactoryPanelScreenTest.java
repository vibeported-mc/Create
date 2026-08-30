package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The factory gauge's two screens, which are reached one after the other from the same click.
 * <p>
 * A panel with nothing on it yet answers an empty-handed click by asking what it is for, which is the
 * first screen: a single ghost slot. Once it knows, the same click opens the second screen instead, where
 * the panel is given the address it orders from and how long a promise is waited on.
 * <p>
 * So the test clicks the panel twice, and both screens have to appear in turn. The panel is asked what it
 * was left with after each - the item after the first, the address and the waiting time after the second.
 * <p>
 * A gauge holds four panels in one block and which one is clicked depends on where the face was hit, so
 * rather than name one, the test reads back whichever of the four ended up with something on it. Both
 * clicks are aimed identically, so both reach the same one.
 */
@SharedWorld
public class FactoryPanelScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	/** The panel's own slot, which the menu lays out after the player's inventory. */
	private static final int FILTER_SLOT = 36;

	private static final Item MAKES = Items.COBBLESTONE;
	private static final String ADDRESS = "Quarry";

	@ClientGameTest(screenshot = false)
	@DisplayName("A factory panel is told what it makes on one screen and how to order it on the next")
	void setsItsItemAndThenItsOrder(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, wall(), 4);
		server.runCommand("setblock %d %d %d minecraft:stone".formatted(wall().getX(), wall().getY(),
			wall().getZ()));

		server.runCommand("item replace entity @a hotbar.0 with create:factory_gauge");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:cobblestone 1");
		server.runCommand("item replace entity @a hotbar.2 with minecraft:air");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// A gauge refuses to be placed until it has been tuned to a logistics network, which in play is
		// done by clicking a stock link with it. Given one here rather than built, since what is being
		// tested is the panel's screens and not the network they order over.
		server.runOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);
			LogisticallyLinkedBlockItem.assignFrequency(player.getMainHandItem(), player, UUID.randomUUID());
		});
		context.waitTicks(SETTLE_TICKS);

		// Hung on the wall the way a player hangs one. A gauge cannot be conjured into place: a block with
		// none of its four panels switched on takes itself back off the wall, and it is the placing that
		// switches one on.
		ScreenTesting.rightClickAt(context, server, quarter(), wall());
		context.waitTicks(SETTLE_TICKS);

		boolean onTheWall = server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockEntity(panel()) instanceof FactoryPanelBlockEntity);

		assertTrue(onTheWall, "The gauge is not on the wall where the test expects it");

		// An empty hand from here on: anything held would be offered as the panel's item, or would hang a
		// second gauge, instead of opening the screen that asks what the panel is for.
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(2));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickAt(context, server, quarter(), panel());
		ScreenTesting.waitForScreen(context, FactoryPanelSetItemScreen.class);
		context.takeScreenshot(shot("factory_panel_set_item"));

		// Two clicks, as a player makes them: the cobblestone up out of the inventory, then onto the panel.
		ScreenTesting.click(context, ScreenTesting.slot(context, ScreenTesting.slotHolding(context, MAKES)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FILTER_SLOT));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		Item onThePanel = server.computeOnServer(minecraftServer -> configuredPanel(minecraftServer).getFilter()
			.getItem());

		assertEquals(MAKES, onThePanel, "The item put onto the panel did not reach it");

		// Now that it knows what it is for, the same click opens the other screen.
		ScreenTesting.rightClickAt(context, server, quarter(), panel());
		ScreenTesting.waitForScreen(context, FactoryPanelScreen.class);
		context.takeScreenshot(shot("factory_panel_opened"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "addressBox"));
		context.getInput()
			.typeChars(ADDRESS);
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "promiseExpiration"), 2);
		context.takeScreenshot(shot("factory_panel_configured"));

		int shownExpiry = ScreenTesting.scrollState(context, "promiseExpiration");

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		String addressOnPanel = server.computeOnServer(minecraftServer -> (String) ScreenTesting
			.read(configuredPanel(minecraftServer), "recipeAddress"));

		assertEquals(ADDRESS, addressOnPanel, "The address typed onto the panel did not reach it");
		int expiryOnPanel = server.computeOnServer(minecraftServer -> (Integer) ScreenTesting
			.read(configuredPanel(minecraftServer), "promiseClearingInterval"));

		assertEquals(shownExpiry, expiryOnPanel, "The waiting time the screen showed did not reach the panel");
	}

	/** Whichever of the block's four panels was the one clicked, found by it being the one set up. */
	private FactoryPanelBehaviour configuredPanel(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(panel());

		if (!(be instanceof FactoryPanelBlockEntity panelBe))
			throw new AssertionError("There is no factory gauge at " + panel() + " but " + be);

		for (FactoryPanelBehaviour behaviour : panelBe.panels.values())
			if (!behaviour.getFilter()
				.isEmpty())
				return behaviour;

		throw new AssertionError("None of the gauge's four panels was given an item");
	}

	private BlockPos wall() {
		return new BlockPos(60, -57, 60);
	}

	/** The panel itself, hung on the south face of the wall. */
	private BlockPos panel() {
		return wall().south();
	}

	/**
	 * The spot on the wall's face that every click here is aimed at.
	 * <p>
	 * Off the middle on purpose: a gauge holds four panels in one block and the middle of the face is the
	 * corner where all four meet. Aiming at the same quarter each time is what puts the panel there and
	 * then opens that same one.
	 */
	private Vec3 quarter() {
		return new Vec3(wall().getX() + 0.3, wall().getY() + 0.3, wall().getZ() + 1);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
