package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;

/**
 * The clipboard, which is a screen a player types into rather than one they click through.
 * <p>
 * A fresh clipboard opens on an empty first line already being edited, so typing goes straight onto the
 * page. What is typed lives only on the client until the screen is closed, at which point the whole
 * clipboard is sent across and written onto the item in the player's hotbar.
 * <p>
 * So the test writes a line, closes, and asks the server what the item says - and then opens the same
 * clipboard again, which fills the screen from the item rather than from anything the first screen left
 * behind, and is what proves the whole round trip rather than only the sending half of it.
 */
@SharedWorld
public class ClipboardScreenTest {

	private static final int SETTLE_TICKS = 5;

	/** Long enough for the edit to have been sent and taken. */
	private static final int SEND_TICKS = 20;

	private static final String WRITTEN = "sixteen andesite";

	@ClientGameTest(screenshot = false)
	@DisplayName("What is typed onto a clipboard is on the item, and is there again when it is reopened")
	void keepsWhatWasTyped(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("item replace entity @a hotbar.0 with create:clipboard");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// A clipboard opens on a plain right-click; sneaking at one places it as a block instead.
		ScreenTesting.rightClick(context);

		ScreenTesting.waitForScreen(context, ClipboardScreen.class);
		context.takeScreenshot(shot("clipboard_opened"));

		context.getInput()
			.typeChars(WRITTEN);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("clipboard_typed"));

		close(context);

		List<String> lines = linesOnTheItem(server);

		assertTrue(lines.contains(WRITTEN),
			"What was typed onto the clipboard did not reach the item, which reads " + lines);

		ScreenTesting.rightClick(context);
		ClipboardScreen reopened = ScreenTesting.waitForScreen(context, ClipboardScreen.class);
		context.takeScreenshot(shot("clipboard_reopened"));

		List<String> shown = context.computeOnClient(client -> textOf(ClipboardEntry.readAll(reopened.content)));

		close(context);

		assertEquals(List.of(WRITTEN), shown, "The clipboard did not open again on what was written on it");
	}

	/** The screen's own close button, rather than escape, since that is the one a player reaches for. */
	private void close(ClientGameTestContext context) {
		ScreenTesting.click(context, ScreenTesting.widget(context, "closeBtn"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);
	}

	/** Every line of every page of the clipboard in the player's hand, read on the server. */
	private List<String> linesOnTheItem(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);

			return textOf(ClipboardEntry.readAll(player.getMainHandItem()));
		});
	}

	private static List<String> textOf(List<List<ClipboardEntry>> pages) {
		return pages.stream()
			.flatMap(List::stream)
			.map(entry -> entry.text.getString())
			.toList();
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
