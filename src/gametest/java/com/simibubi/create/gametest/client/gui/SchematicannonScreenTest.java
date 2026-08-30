package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;

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
 * The schematicannon's placement settings, which are the part of its screen that works without a
 * schematic to print.
 * <p>
 * They are hidden until asked for, so the test opens them first, then picks a replacement rule and turns
 * the two other options over. Each of those buttons sends on its own rather than waiting for the screen
 * to close, so the block can be asked about each one as it goes.
 * <p>
 * Nothing is fired: what is being tested is the screen, and the cannon answers these settings whether or
 * not it has anything to build.
 */
@SharedWorld
public class SchematicannonScreenTest {

	private static final int SETTLE_TICKS = 10;

	/**
	 * Replace with any, the third of the four rules.
	 * <p>
	 * Not the one a cannon starts on, so clicking it is a change rather than a click that does nothing -
	 * the buttons only send when the rule would really move.
	 */
	private static final int REPLACE_ANY = 2;

	@ClientGameTest(screenshot = false)
	@DisplayName("The placement settings chosen on the schematicannon's screen reach the cannon")
	void configuresThePlacementSettings(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, cannon(), 4);
		server.runCommand("setblock %d %d %d create:schematicannon".formatted(cannon().getX(), cannon().getY(),
			cannon().getZ()));
		context.waitTicks(SETTLE_TICKS);

		boolean skippedMissingBefore = flag(server, be -> be.skipMissing);
		boolean replacedBlockEntitiesBefore = flag(server, be -> be.replaceBlockEntities);

		ScreenTesting.rightClickBlock(context, server, cannon());
		ScreenTesting.waitForScreen(context, SchematicannonScreen.class);
		context.takeScreenshot(shot("schematicannon_opened"));

		// The placement settings are not on screen until asked for, and the buttons do not exist until then.
		ScreenTesting.click(context, ScreenTesting.widget(context, "showSettingsButton"));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("schematicannon_settings_shown"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "replaceLevelButtons", REPLACE_ANY));
		ScreenTesting.click(context, ScreenTesting.widget(context, "skipMissingButton"));
		ScreenTesting.click(context, ScreenTesting.widget(context, "skipBlockEntitiesButton"));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("schematicannon_configured"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);

		assertEquals(REPLACE_ANY, number(server, be -> be.replaceMode),
			"The replacement rule chosen on the screen did not reach the cannon");
		assertNotEquals(skippedMissingBefore, flag(server, be -> be.skipMissing),
			"Turning over whether to skip missing blocks did not reach the cannon");
		assertNotEquals(replacedBlockEntitiesBefore, flag(server, be -> be.replaceBlockEntities),
			"Turning over whether to replace block entities did not reach the cannon");
	}

	private BlockPos cannon() {
		return new BlockPos(60, -58, 60);
	}

	private int number(TestServerContext server, ToIntFunction<SchematicannonBlockEntity> read) {
		return server.computeOnServer(minecraftServer -> read.applyAsInt(theCannon(minecraftServer)));
	}

	private boolean flag(TestServerContext server, Predicate<SchematicannonBlockEntity> read) {
		return server.computeOnServer(minecraftServer -> read.test(theCannon(minecraftServer)));
	}

	private SchematicannonBlockEntity theCannon(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(cannon());

		if (!(be instanceof SchematicannonBlockEntity cannonBe))
			throw new AssertionError("There is no schematicannon at " + cannon() + " but " + be);

		return cannonBe;
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
