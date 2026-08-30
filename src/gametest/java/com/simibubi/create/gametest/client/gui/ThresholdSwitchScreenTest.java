package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchScreen;

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
 * The threshold switch's screen: the two levels it turns on and off at, whether they are counted in
 * items or in stacks, and which way round the signal runs.
 * <p>
 * The switch is put on a chest, since the levels it offers to be set to are worked out from whatever it
 * is watching, and a switch watching nothing has nothing to offer.
 * <p>
 * Both thresholds are read off the screen rather than named here: the two are tied together - moving one
 * past the other pushes the other along - so what a given number of notches leaves them at is the
 * screen's business. What is being checked is that whatever the screen showed is what the block was left
 * with.
 */
@SharedWorld
public class ThresholdSwitchScreenTest {

	private static final int SETTLE_TICKS = 5;

	/** Long enough for the screen's own packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	@ClientGameTest(screenshot = false)
	@DisplayName("The levels set on a threshold switch's screen are the ones the block is left with")
	void configuresTheThresholds(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, chest(), 4);
		server.runCommand("setblock %d %d %d minecraft:chest".formatted(chest().getX(), chest().getY(),
			chest().getZ()));
		server.runCommand("setblock %d %d %d create:stockpile_switch[target=floor,facing=north]".formatted(switchPos().getX(),
			switchPos().getY(), switchPos().getZ()));

		// Something for it to measure, so the screen has a range worth scrolling through.
		server.runCommand("item replace block %d %d %d container.0 with minecraft:cobblestone 64"
			.formatted(chest().getX(), chest().getY(), chest().getZ()));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, switchPos());
		ScreenTesting.waitForScreen(context, ThresholdSwitchScreen.class);
		context.takeScreenshot(shot("threshold_switch_opened"));

		ScreenTesting.scroll(context, ScreenTesting.widget(context, "onAbove"), 2);
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "offBelow"), 1);
		context.takeScreenshot(shot("threshold_switch_configured"));

		int shownOnAbove = ScreenTesting.scrollState(context, "onAbove");
		int shownOffBelow = ScreenTesting.scrollState(context, "offBelow");

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertTrue(shownOnAbove > shownOffBelow,
			"The screen left the upper threshold at or below the lower one, so nothing useful was set");
		assertEquals(shownOnAbove, number(server, be -> be.onWhenAbove),
			"The upper threshold the screen showed did not reach the block");
		assertEquals(shownOffBelow, number(server, be -> be.offWhenBelow),
			"The lower threshold the screen showed did not reach the block");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("A threshold switch counts in stacks when its screen is set to, and can be inverted")
	void countsInStacksAndInverts(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, chest(), 4);
		server.runCommand("setblock %d %d %d minecraft:chest".formatted(chest().getX(), chest().getY(),
			chest().getZ()));
		server.runCommand("setblock %d %d %d create:stockpile_switch[target=floor,facing=north]".formatted(switchPos().getX(),
			switchPos().getY(), switchPos().getZ()));
		context.waitTicks(SETTLE_TICKS);

		boolean invertedBefore = flag(server, be -> be.isInverted());

		ScreenTesting.rightClickBlock(context, server, switchPos());
		ScreenTesting.waitForScreen(context, ThresholdSwitchScreen.class);

		// Items to stacks, one option down the list.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "inStacks"), -1);

		// The flip button sends on its own rather than waiting for the screen to be closed.
		ScreenTesting.click(context, ScreenTesting.widget(context, "flipSignals"));
		context.waitTicks(SEND_TICKS);
		context.takeScreenshot(shot("threshold_switch_flipped"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertNotEquals(invertedBefore, flag(server, be -> be.isInverted()),
			"Flipping the signal on the screen did not reach the block");
		assertTrue(flag(server, be -> be.inStacks),
			"Choosing to count in stacks did not reach the block");
	}

	/**
	 * Where the switch is: on top of the chest it watches.
	 * <p>
	 * Which way it looks is not a facing but the face it is attached by - stood on the floor it looks
	 * down, which from up here means down into the chest.
	 */
	private BlockPos switchPos() {
		return chest().above();
	}

	private BlockPos chest() {
		return new BlockPos(60, -58, 60);
	}

	private int number(TestServerContext server, ToIntFunction<ThresholdSwitchBlockEntity> read) {
		return server.computeOnServer(minecraftServer -> read.applyAsInt(theSwitch(minecraftServer)));
	}

	private boolean flag(TestServerContext server, Predicate<ThresholdSwitchBlockEntity> read) {
		return server.computeOnServer(minecraftServer -> read.test(theSwitch(minecraftServer)));
	}

	private ThresholdSwitchBlockEntity theSwitch(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(switchPos());

		if (!(be instanceof ThresholdSwitchBlockEntity switchBe))
			throw new AssertionError("There is no threshold switch at " + switchPos() + " but " + be);

		return switchBe;
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
