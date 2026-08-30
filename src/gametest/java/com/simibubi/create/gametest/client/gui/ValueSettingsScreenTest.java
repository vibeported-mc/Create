package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The value board, which is the little dial that dozens of Create's blocks share - funnels, chutes,
 * tunnels, pipes, and the speed controller used here.
 * <p>
 * It is the only screen in the mod that is not opened by a click. The button is held down while the
 * crosshair rests on the small box drawn on the block, and after a few ticks the board appears; the
 * cursor is then dragged along it and what is set is whatever it was resting on when the button was let
 * go. So the test holds, drags and releases rather than clicking anything.
 * <p>
 * A speed controller is a fair stand-in for the rest: its box sits on whichever side is not along its
 * axis, and what the board sets is a plain number that can be read straight back off the block.
 */
@SharedWorld
public class ValueSettingsScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the board to have appeared, which takes six ticks of holding. */
	private static final int WARMUP_TICKS = 15;

	/** Long enough for the board's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	/** How far along the bar to drag, in the board's own steps. */
	private static final int COLUMN = 12;

	@ClientGameTest(screenshot = false)
	@DisplayName("The value dragged out on a block's board is the value the block is left set to")
	void dragsOutAValue(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, controller(), 4);

		// Laid along x, which is what puts its little box on the side facing the player.
		server.runCommand("setblock %d %d %d create:rotation_speed_controller[axis=x]".formatted(controller().getX(),
			controller().getY(), controller().getZ()));
		context.waitTicks(SETTLE_TICKS);

		int before = speed(server);

		// Held rather than clicked, and held on the box itself: anywhere else on the block and the button
		// is just an ordinary click that never brings the board up.
		ScreenTesting.holdRightClickAt(context, server, valueBox(), controller());

		ValueSettingsScreen screen = ScreenTesting.waitForScreen(context, ValueSettingsScreen.class);
		context.waitTicks(WARMUP_TICKS);
		context.takeScreenshot(shot("value_settings_opened"));

		// The board says where each of its steps is drawn, so the cursor is dragged to one of them rather
		// than to a place worked out here.
		Vec3 target = context.computeOnClient(client -> {
			var coordinate = screen.getCoordinateOfValue(0, COLUMN);
			return new Vec3(coordinate.x, coordinate.y, 0);
		});

		ScreenTesting.hoverAt(context, target.x, target.y);
		context.takeScreenshot(shot("value_settings_dragged"));

		// Letting go is what sets it.
		context.getInput()
			.releaseMouse(1);
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertNotEquals(before, speed(server),
			"Dragging the board out and letting go did not change what the block is set to");
	}

	private int speed(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(controller());

			if (!(be instanceof SpeedControllerBlockEntity controllerBe))
				throw new AssertionError("There is no speed controller at " + controller() + " but " + be);

			return controllerBe.targetSpeed.getValue();
		});
	}

	private BlockPos controller() {
		return new BlockPos(60, -58, 60);
	}

	/**
	 * Where the little box is drawn on the block, which is the only place the board can be summoned from.
	 * <p>
	 * Near the top of the face and just outside it, so the crosshair lands on the box rather than passing
	 * through into the block behind.
	 */
	private Vec3 valueBox() {
		return new Vec3(controller().getX() + 0.5, controller().getY() + 11 / 16d, controller().getZ() + 0.97);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
