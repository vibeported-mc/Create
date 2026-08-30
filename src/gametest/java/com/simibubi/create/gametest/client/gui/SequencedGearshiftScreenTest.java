package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.transmission.sequencer.Instruction;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The sequenced gearshift's screen: five rows of what to do, how far, and how fast.
 * <p>
 * A gearshift starts on a single instruction - turn ninety degrees - so the first row is the one worth
 * driving. Its instruction is scrolled onto something else, which changes what the rest of the row means,
 * and then its value is scrolled as well.
 * <p>
 * What the row ends up saying is read off the screen rather than named here, since which instruction is
 * one place down the list, and what values that one allows, are the screen's business. The block is then
 * asked what it was left with, and the two have to agree.
 */
@SharedWorld
public class SequencedGearshiftScreenTest {

	private static final int SETTLE_TICKS = 5;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	/** The row a fresh gearshift already has an instruction on. */
	private static final int FIRST_ROW = 0;

	private static final int INSTRUCTION = 0;
	private static final int VALUE = 1;

	@ClientGameTest(screenshot = false)
	@DisplayName("The instruction set on a sequenced gearshift's screen is the one the block is left with")
	void configuresTheFirstInstruction(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, gearshift(), 4);
		server.runCommand("setblock %d %d %d create:sequenced_gearshift".formatted(gearshift().getX(),
			gearshift().getY(), gearshift().getZ()));
		context.waitTicks(SETTLE_TICKS);

		int instructionBefore = onTheBlock(server, "instruction");

		ScreenTesting.rightClickBlock(context, server, gearshift());
		ScreenTesting.waitForScreen(context, SequencedGearshiftScreen.class);
		context.takeScreenshot(shot("sequenced_gearshift_opened"));

		// One instruction down the list, which also changes what the rest of the row means, and then a
		// couple of notches on the value the new instruction takes.
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "inputs", FIRST_ROW, INSTRUCTION), -1);
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "inputs", FIRST_ROW, VALUE), 2);
		context.takeScreenshot(shot("sequenced_gearshift_configured"));

		int shownInstruction = ScreenTesting.scrollState(context, "inputs", FIRST_ROW, INSTRUCTION);
		int shownValue = ScreenTesting.scrollState(context, "inputs", FIRST_ROW, VALUE);

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertNotEquals(instructionBefore, shownInstruction,
			"Scrolling the instruction did not move it off the one the gearshift started on");
		assertEquals(shownInstruction, onTheBlock(server, "instruction"),
			"The instruction the screen showed is not the one the gearshift was left with");
		assertEquals(shownValue, onTheBlock(server, "value"),
			"The value the screen showed did not reach the gearshift");
	}

	private BlockPos gearshift() {
		return new BlockPos(60, -58, 60);
	}

	/**
	 * What the gearshift's first instruction says, as a number.
	 * <p>
	 * An instruction keeps its parts to itself, so they are read rather than asked for - and which kind
	 * of instruction it is comes back as its place in the list, which is what the screen's control holds.
	 */
	private int onTheBlock(TestServerContext server, String part) {
		return server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(gearshift());

			if (!(be instanceof SequencedGearshiftBlockEntity gearshiftBe))
				throw new AssertionError("There is no sequenced gearshift at " + gearshift() + " but " + be);

			Instruction instruction = gearshiftBe.getInstructions()
				.get(FIRST_ROW);
			Object value = ScreenTesting.read(instruction, part);

			return value instanceof Enum<?> option ? option.ordinal() : (Integer) value;
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
