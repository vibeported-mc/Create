package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.equipment.goggles.GoggleConfigScreen;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * The screen that says where the goggle overlay sits.
 * <p>
 * It is the only screen here opened by a command rather than by holding, clicking or pressing anything -
 * {@code /create overlay}. Clicking anywhere on it moves the overlay to that spot, and closing the screen
 * is what writes where it ended up into the client's own settings.
 * <p>
 * So the test clicks off to one side and reads the offset back out of those settings, which is where the
 * overlay itself looks for it.
 */
@SharedWorld
public class GoggleConfigScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** How far off centre to put it, in the screen's own units. */
	private static final int MOVED_BY = 40;

	@ClientGameTest(screenshot = false)
	@DisplayName("Clicking the goggle overlay to a new place is where it is remembered")
	void movesTheOverlay(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		// Back to where it started, so that the move has somewhere to move it from.
		sendCommand(context, "create overlay reset");
		context.waitTicks(SETTLE_TICKS);

		int before = context.computeOnClient(client -> AllConfigs.client().overlayOffsetX.get());

		sendCommand(context, "create overlay");
		ScreenTesting.waitForScreen(context, GoggleConfigScreen.class);
		context.takeScreenshot(shot("goggle_overlay_opened"));

		// Put down off to one side of the middle, which is the whole of what this screen does.
		int middleX = context.computeOnClient(client -> client.getWindow()
			.getGuiScaledWidth() / 2);
		int middleY = context.computeOnClient(client -> client.getWindow()
			.getGuiScaledHeight() / 2);

		ScreenTesting.clickAt(context, middleX + MOVED_BY, middleY);
		context.takeScreenshot(shot("goggle_overlay_moved"));

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);

		int after = context.computeOnClient(client -> AllConfigs.client().overlayOffsetX.get());

		assertNotEquals(before, after, "Moving the overlay across the screen did not move where it is remembered");
	}

	/** Typed by the player rather than run by the server, since it is the player's own screen it opens. */
	private static void sendCommand(ClientGameTestContext context, String command) {
		context.runOnClient(client -> client.player.connection.sendCommand(command));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
