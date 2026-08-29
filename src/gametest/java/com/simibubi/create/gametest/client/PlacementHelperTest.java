package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllBlocks;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Holding a cogwheel and looking at a shaft, which is what Create's placement helpers are for.
 * <p>
 * Two things should be on screen: the ghost of the cogwheel where it would go, and the little arrow
 * at the crosshair pointing at it. The ghost is drawn in the world and the arrow is a hud element,
 * and those arrive by completely different routes - the arrow went missing in the port because the
 * game asks mods for their hud layers earlier than it used to, and Catnip was registering its one
 * after that. So the picture in the report is the thing to look at here: the arrow is the half that
 * has already been lost once.
 */
public class PlacementHelperTest {

	private static final BlockPos SHAFT = new BlockPos(3, -60, 0);

	@ClientGameTest(screenshot = false)
	@DisplayName("A cogwheel offers to go on a shaft")
	void cogwheelOnShaft(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");
		server.runCommand("setblock %d %d %d create:shaft[axis=x]".formatted(SHAFT.getX(), SHAFT.getY(),
			SHAFT.getZ()));
		server.runCommand("item replace entity @a hotbar.0 with create:cogwheel");
		// On the ground a few blocks south of the shaft, looking down at the middle of it.
		server.runCommand("tp @a %.1f %d %.1f 180 20".formatted(SHAFT.getX() + 0.5, SHAFT.getY(),
			SHAFT.getZ() + 3.5));

		// Long enough for the helpers to notice, and for the indicator to finish fading in.
		context.waitTicks(20);

		context.runOnClient(client -> {
			BlockHitResult hit = assertInstanceOf(BlockHitResult.class, client.hitResult,
				"The player is not looking at a block, so no placement helper would run");

			if (!AllBlocks.SHAFT.has(client.level.getBlockState(hit.getBlockPos()))) {
				throw new AssertionError("The player is looking at " + hit.getBlockPos() + ", not the shaft");
			}
		});

		context.takeScreenshot(TestScreenshotOptions.of("placement_helper")
			.withSize(854, 480));
	}

}
