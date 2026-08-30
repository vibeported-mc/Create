package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;

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
 * The postbox's screen, which is where a package port is given the address it answers to.
 * <p>
 * A postbox opens on an empty-handed click; a clipboard in hand copies the address instead of opening
 * anything, which is why the hand is left empty here. The address is typed into the box and is sent when
 * the screen closes, so it is read back off the block: an address that never left the screen is one no
 * package would ever be sorted by.
 * <p>
 * The send-only and send-and-receive buttons are not touched. A port only shows them once it has a target
 * to deliver into - a chain conveyor or a train station - and a postbox stood on the ground has none, so
 * on this one they are not on screen to be clicked.
 */
@SharedWorld
public class PackagePortScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	private static final String ADDRESS = "Smeltery";

	@ClientGameTest(screenshot = false)
	@DisplayName("The address typed onto a postbox is the one the block is left answering to")
	void keepsItsAddress(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, postbox(), 4);
		server.runCommand("setblock %d %d %d create:blue_postbox[facing=south]".formatted(postbox().getX(),
			postbox().getY(), postbox().getZ()));

		// An empty hand, since a clipboard would copy the address rather than open the screen.
		server.runCommand("item replace entity @a hotbar.0 with minecraft:air");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, postbox());
		ScreenTesting.waitForScreen(context, PackagePortScreen.class);
		context.takeScreenshot(shot("package_port_opened"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "addressBox"));
		context.getInput()
			.typeChars(ADDRESS);
		context.waitTicks(SETTLE_TICKS);

		context.takeScreenshot(shot("package_port_addressed"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		assertEquals(ADDRESS, address(server), "The address typed onto the postbox did not reach the block");
	}

	private String address(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> thePostbox(minecraftServer).addressFilter);
	}

	private PackagePortBlockEntity thePostbox(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(postbox());

		if (!(be instanceof PackagePortBlockEntity portBe))
			throw new AssertionError("There is no postbox at " + postbox() + " but " + be);

		return portBe;
	}

	private BlockPos postbox() {
		return new BlockPos(60, -58, 60);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
