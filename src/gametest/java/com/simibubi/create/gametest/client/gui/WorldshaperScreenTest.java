package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.zapper.terrainzapper.PlacementOptions;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainBrushes;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainTools;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Creative Worldshaper's screen, opened and worked the way a player opens and works it.
 * <p>
 * Nothing here reaches into the screen to make it do things: the item goes into the hand, the sneak key
 * goes down, the right mouse button is pressed, and the controls are clicked and scrolled at where they
 * are on screen. So a screen that stopped opening, or a button that stopped taking clicks, fails this
 * test rather than quietly passing it.
 * <p>
 * What the screen is for is writing settings onto the item it was opened from, and that is what is
 * checked at the end - on the server, where the item really lives.
 */
public class WorldshaperScreenTest {

	/** How long a menu or a deferred screen has to appear before something is wrong. */
	private static final int SETTLE_TICKS = 5;

	@ClientGameTest(screenshot = false)
	@DisplayName("Sneaking and right-clicking the Worldshaper opens its screen, and the settings stick")
	void configuresTheHeldItem(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with create:handheld_worldshaper");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.sneakRightClick(context);

		WorldshaperScreen screen = ScreenTesting.waitForScreen(context, WorldshaperScreen.class);
		context.takeScreenshot(shot("worldshaper_opened"));

		// A cuboid to start with, and a sphere one place further down the list - so the wheel turns down,
		// the way a player reaching for the next option turns it. A list of options is wired the other
		// way round to a number, which is why this is negative and the parameter below is not.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "brushInput"), -1);

		// The parameters belong to the brush, so they are only worth touching once it has been picked.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "brushParams", 0), 2);

		ScreenTesting.click(context, ScreenTesting.widget(context, "toolButtons", 1));
		ScreenTesting.click(context, ScreenTesting.widget(context, "placementButtons", 1));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("worldshaper_configured"));

		// What the screen believes it is set to, to be held against what the item ends up with.
		TerrainBrushes shownBrush = (TerrainBrushes) ScreenTesting.read(screen, "currentBrush");
		TerrainTools shownTool = (TerrainTools) ScreenTesting.read(screen, "currentTool");
		PlacementOptions shownPlacement = (PlacementOptions) ScreenTesting.read(screen, "currentPlacement");

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);

		assertEquals(TerrainBrushes.Sphere, shownBrush, "Scrolling the brush input did not change the brush");
		assertNotEquals(TerrainTools.Fill, shownTool, "Clicking the second tool button did not change the tool");
		assertNotEquals(PlacementOptions.Merged, shownPlacement,
			"Clicking the second placement button did not change the placement");

		// The screen writes what it was set to onto the item as it closes, so the item is the proof.
		assertEquals(shownBrush, held(server, AllDataComponents.SHAPER_BRUSH, TerrainBrushes.Cuboid),
			"The brush the screen showed is not the brush the item was left with");
		assertEquals(shownTool, held(server, AllDataComponents.SHAPER_TOOL, TerrainTools.Fill),
			"The tool the screen showed is not the tool the item was left with");
		assertEquals(shownPlacement,
			held(server, AllDataComponents.SHAPER_PLACEMENT_OPTIONS, PlacementOptions.Merged),
			"The placement the screen showed is not the placement the item was left with");
	}

	/** What the item in the player's hand carries under this component, on the server. */
	private <T> T held(TestServerContext server, net.minecraft.core.component.DataComponentType<T> component,
		T fallback) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);
			ItemStack worldshaper = player.getMainHandItem();

			return worldshaper.getOrDefault(component, fallback);
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
