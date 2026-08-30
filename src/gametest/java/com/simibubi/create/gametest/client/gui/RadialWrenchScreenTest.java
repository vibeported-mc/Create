package com.simibubi.create.gametest.client.gui;

import org.junit.jupiter.api.DisplayName;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.simibubi.create.AllKeys;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

/**
 * The wrench's rotation menu, which offers every way a block can be turned rather than stepping through
 * them one click at a time.
 * <p>
 * It is the one screen in the mod that no key opens out of the box: the binding ships unassigned, so a
 * player has to give it a key in the options before it can be used at all. The test does the same before
 * pressing it, which is the only way to reach the screen and is worth knowing still works.
 * <p>
 * What it offers depends on the block being looked at, so the block here is a staircase - it has a facing,
 * a half and a shape, which is three of the things the menu knows how to turn.
 */
@SharedWorld
public class RadialWrenchScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Some key to give the binding, since it ships without one. */
	private static final int BOUND_TO = GLFW.GLFW_KEY_R;

	@ClientGameTest(screenshot = false)
	@DisplayName("Once its key is bound, the wrench's rotation menu opens on the block being looked at")
	void opensOnTheBlockBeingLookedAt(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, stairs(), 4);
		server.runCommand("setblock %d %d %d minecraft:oak_stairs[facing=south]".formatted(stairs().getX(),
			stairs().getY(), stairs().getZ()));
		server.runCommand("item replace entity @a hotbar.0 with create:wrench");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// What a player does in the options screen before any of this works.
		context.runOnClient(client -> {
			AllKeys.ROTATE_MENU.getKeybind()
				.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(BOUND_TO));
			KeyMapping.resetMapping();
		});
		context.waitTicks(SETTLE_TICKS);

		// The menu is about whatever is under the crosshair, so the player is put in front of it first.
		ScreenTesting.lookAtBlock(context, server, stairs());
		context.getInput()
			.pressKey(BOUND_TO);

		ScreenTesting.waitForScreen(context, RadialWrenchMenu.class);
		context.takeScreenshot(shot("radial_wrench"));

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);
	}

	private BlockPos stairs() {
		return new BlockPos(60, -58, 60);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
