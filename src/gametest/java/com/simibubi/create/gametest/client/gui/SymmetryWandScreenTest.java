package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandScreen;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Wand of Symmetry's screen, which picks the kind of mirror the wand works with and how it is turned.
 * <p>
 * Unlike the other held-item screens, this one does not write to the stack in the player's hand and leave
 * it at that: the copy the screen was handed is a client one, so it also sends what was chosen to the
 * server. That makes the server-side item the only thing worth asserting on - a screen that set itself up
 * correctly but stopped sending would still leave the wand doing nothing.
 * <p>
 * Which mirror the screen ends up on is read off the screen and held against the item, rather than named
 * here, so this stays true if the list of mirrors is ever reordered or added to.
 */
@SharedWorld
public class SymmetryWandScreenTest {

	private static final int SETTLE_TICKS = 5;

	@ClientGameTest(screenshot = false)
	@DisplayName("The mirror the symmetry wand's screen was set to reaches the wand on the server")
	void configuresTheMirror(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with create:wand_of_symmetry");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.sneakRightClick(context);

		SymmetryWandScreen screen = ScreenTesting.waitForScreen(context, SymmetryWandScreen.class);
		context.takeScreenshot(shot("symmetry_wand_opened"));

		// One mirror further down the list, which turns the single plane into the crossed pair, and then
		// one alignment further down - the crossed pair is the mirror that has more than one.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "areaType"), -1);
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "areaAlign"), -1);
		context.takeScreenshot(shot("symmetry_wand_configured"));

		// Picking a mirror builds a new one, so this has to be read after the scrolling rather than before.
		SymmetryMirror shown = (SymmetryMirror) ScreenTesting.read(screen, "currentElement");

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);

		SymmetryMirror onTheWand = held(server);

		assertNotNull(onTheWand, "Closing the screen did not leave a mirror on the wand at all");
		assertEquals(shown.getClass(), onTheWand.getClass(),
			"The mirror the screen showed is not the kind the wand was left with");
		assertEquals(shown.getOrientationIndex(), onTheWand.getOrientationIndex(),
			"The alignment the screen showed did not reach the wand");
	}

	/** The mirror on the wand in the player's hand, on the server, where the wand really lives. */
	private SymmetryMirror held(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);

			return player.getMainHandItem()
				.get(AllDataComponents.SYMMETRY_WAND);
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
