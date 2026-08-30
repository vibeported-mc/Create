package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * The screen that says where a schematic is to be laid down, and which way round.
 * <p>
 * The schematic here names a file that was never uploaded, which is deliberate: the screen and everything
 * behind it is reached without needing a real structure on disk, since nothing is loaded until the
 * schematic is actually deployed and a schematic that reports no size is dropped before that happens.
 * What is being tested is the placement, not the printing.
 * <p>
 * Closing the screen does not write to the item directly. It hands the placement to the client's schematic
 * handler, which sends it up a short while later, and the server writes it onto the stack in the hotbar
 * slot the schematic was held in - so the assertions wait for that to have happened.
 */
@SharedWorld
public class SchematicEditScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Longer than the handler's own delay before it sends what it was given. */
	private static final int SYNC_TICKS = 30;

	@ClientGameTest(screenshot = false)
	@DisplayName("The placement set on a schematic's screen reaches the schematic on the server")
	void configuresThePlacement(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("gamemode creative @a");

		// A named file and a size, which is all the screen needs: without a size the handler has no bounds
		// to place anything within, and without a name it does not take the schematic as the active one.
		server.runCommand("item replace entity @a hotbar.0 with "
			+ "create:schematic[create:schematic_file=\"gametest.nbt\",create:schematic_bounds=[I;3,3,3]]");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.sneakRightClick(context);

		SchematicEditScreen screen = ScreenTesting.waitForScreen(context, SchematicEditScreen.class);
		context.takeScreenshot(shot("schematic_edit_opened"));

		// One turn and one mirror down each list, which is as far as a player scrolls to get off "none".
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "rotationArea"), -1);
		ScreenTesting.scroll(context, ScreenTesting.widget(context, "mirrorArea"), -1);
		context.takeScreenshot(shot("schematic_edit_configured"));

		// Where the screen says it will go. Undeployed, it offers the player's own position, so this is
		// read off the screen rather than worked out again here.
		BlockPos shownAnchor = anchorShownOn(context, screen);

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SYNC_TICKS);

		assertEquals(Rotation.CLOCKWISE_90, held(server, AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE),
			"The rotation the screen was scrolled to did not reach the schematic");
		assertEquals(Mirror.LEFT_RIGHT, held(server, AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE),
			"The mirror the screen was scrolled to did not reach the schematic");
		assertEquals(shownAnchor, held(server, AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO),
			"The position the screen showed is not where the schematic was left anchored");
		assertEquals(Boolean.TRUE, held(server, AllDataComponents.SCHEMATIC_DEPLOYED, false),
			"Confirming the placement did not leave the schematic deployed");
	}

	/** The three coordinate boxes, read as the position they spell out. */
	private BlockPos anchorShownOn(ClientGameTestContext context, SchematicEditScreen screen) {
		return context.computeOnClient(client -> new BlockPos(coordinate(screen, "xInput"),
			coordinate(screen, "yInput"), coordinate(screen, "zInput")));
	}

	private static int coordinate(SchematicEditScreen screen, String field) {
		return Integer.parseInt(((EditBox) ScreenTesting.read(screen, field)).getValue());
	}

	/** What the schematic in the player's hand carries under this component, on the server. */
	private <T> T held(TestServerContext server, net.minecraft.core.component.DataComponentType<T> component,
		T fallback) {
		return server.computeOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);
			ItemStack schematic = player.getMainHandItem();

			return schematic.getOrDefault(component, fallback);
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
