package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The elevator contact's screen, which is what names a floor.
 * <p>
 * The short name is what the call buttons show and the long one is the description beside it. The screen
 * opens with the short name already being edited and all of it selected, so typing goes straight onto it
 * and replaces what was there, which is the gesture a player makes to rename a floor.
 * <p>
 * The contact is placed on its own rather than as part of a working elevator: what is being tested is the
 * naming, and a contact answers that whether or not there is a pulley above it.
 */
@SharedWorld
public class ElevatorContactScreenTest {

	private static final int SETTLE_TICKS = 5;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	private static final String SHORT_NAME = "3";
	private static final String LONG_NAME = "Smeltery";

	@ClientGameTest(screenshot = false)
	@DisplayName("The names typed onto an elevator contact are the ones the floor is left with")
	void namesTheFloor(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, contact(), 4);
		server.runCommand("setblock %d %d %d create:elevator_contact[facing=south]".formatted(contact().getX(),
			contact().getY(), contact().getZ()));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, contact());
		ScreenTesting.waitForScreen(context, ElevatorContactScreen.class);
		context.takeScreenshot(shot("elevator_contact_opened"));

		// The short name is already being edited, with what was there selected, so this replaces it.
		context.getInput()
			.typeChars(SHORT_NAME);
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.click(context, ScreenTesting.widget(context, "longNameInput"));
		context.getInput()
			.typeChars(LONG_NAME);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("elevator_contact_named"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirm"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertEquals(SHORT_NAME, onTheBlock(server, be -> be.shortName),
			"The short name typed on the screen did not reach the contact");
		assertEquals(LONG_NAME, onTheBlock(server, be -> be.longName),
			"The description typed on the screen did not reach the contact");
	}

	private BlockPos contact() {
		return new BlockPos(60, -58, 60);
	}

	private String onTheBlock(TestServerContext server, Function<ElevatorContactBlockEntity, String> read) {
		return server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(contact());

			if (!(be instanceof ElevatorContactBlockEntity contactBe))
				throw new AssertionError("There is no elevator contact at " + contact() + " but " + be);

			return read.apply(contactBe);
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
