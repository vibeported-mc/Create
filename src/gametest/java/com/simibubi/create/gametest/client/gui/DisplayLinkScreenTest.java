package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The display link's screen, which says what is read and which line of the display it is written on.
 * <p>
 * A link needs something on both sides of it before the screen is worth anything. It refuses to open at
 * all until it has been aimed at something to write on, and it offers nothing to read unless the block it
 * is stood on is one of the things Create knows how to read. A chest is not one of those - a threshold
 * switch watching a chest is, and reports how full the chest is - so the stack here is chest, switch,
 * link, with a sign a few blocks away to write on.
 * <p>
 * The aiming at the sign is done directly rather than by clicking the sign with the link first, since
 * what is being tested is the screen rather than the way a link is bound.
 */
@SharedWorld
public class DisplayLinkScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	@ClientGameTest(screenshot = false)
	@DisplayName("The line chosen on a display link's screen is the line the link is left writing to")
	void configuresTheTargetLine(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, chest(), 5);

		// Something to read from - a switch watching a chest reports how full it is - something to write
		// on, and the link between them.
		setBlock(server, chest(), "minecraft:chest");
		setBlock(server, switchPos(), "create:stockpile_switch[target=floor,facing=north]");
		setBlock(server, link(), "create:display_link[facing=up]");
		setBlock(server, sign(), "minecraft:oak_sign");
		context.waitTicks(SETTLE_TICKS);

		server.runOnServer(minecraftServer -> {
			DisplayLinkBlockEntity linkBe = theLink(minecraftServer);
			linkBe.target(sign());

			// The client decides whether the screen may open at all, so it has to be told as well.
			linkBe.notifyUpdate();
		});
		context.waitTicks(SETTLE_TICKS);

		int lineBefore = targetLine(server);

		ScreenTesting.rightClickBlock(context, server, link());
		ScreenTesting.waitForScreen(context, DisplayLinkScreen.class);
		context.takeScreenshot(shot("display_link_opened"));

		// A list of lines runs the other way round to a number, so down the list is a downward notch.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "targetLineSelector"), -1);
		context.takeScreenshot(shot("display_link_configured"));

		int shownLine = ScreenTesting.scrollState(context, "targetLineSelector");

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertNotEquals(lineBefore, shownLine, "Scrolling the line did not move it off the one it started on");
		assertEquals(shownLine, targetLine(server),
			"The line the screen showed is not the line the link was left writing to");
		assertNotNull(server.computeOnServer(minecraftServer -> theLink(minecraftServer).activeSource),
			"Closing the screen did not leave the link reading anything");
	}

	private BlockPos chest() {
		return new BlockPos(60, -58, 60);
	}

	/** What is read: a switch watching the chest below it. */
	private BlockPos switchPos() {
		return chest().above();
	}

	/**
	 * The link itself, stood on the switch.
	 * <p>
	 * A link's facing is the face of the block it was stuck to, and it reads whatever is on the other
	 * side of that - so one stood on top of something faces up and reads downwards.
	 */
	private BlockPos link() {
		return switchPos().above();
	}

	/** What is written on, a few blocks clear so neither is in the other's way. */
	private BlockPos sign() {
		return new BlockPos(60, -58, 57);
	}

	private int targetLine(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> theLink(minecraftServer).targetLine);
	}

	private DisplayLinkBlockEntity theLink(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(link());

		if (!(be instanceof DisplayLinkBlockEntity linkBe))
			throw new AssertionError("There is no display link at " + link() + " but " + be);

		return linkBe;
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
