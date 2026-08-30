package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The redstone requester's screen, which is a standing order: what to ask for, and where from.
 * <p>
 * The order itself is a row of ghost slots, so an item is carried into the first of them with the cursor.
 * The address it orders from is typed in, and the requester is told to take nothing rather than take what
 * it can get - all three of which are sent together as the screen closes.
 * <p>
 * A requester on its own is not on any network, which is fine: what is being tested is that the order it
 * was given is the order it is left holding, not whether anything ever answers it.
 */
@SharedWorld
public class RedstoneRequesterScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	/** The first slot of the order, which the menu lays out after the player's inventory. */
	private static final int FIRST_ORDER_SLOT = 36;

	private static final String ADDRESS = "Warehouse";

	@ClientGameTest(screenshot = false)
	@DisplayName("The order set on a redstone requester's screen is the order the block is left holding")
	void keepsTheOrderItWasGiven(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, requester(), 4);
		server.runCommand("setblock %d %d %d create:redstone_requester".formatted(requester().getX(),
			requester().getY(), requester().getZ()));

		// An empty hand, since a requester answers a crouching or a holding click differently.
		server.runCommand("item replace entity @a hotbar.0 with minecraft:air");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:cobblestone 1");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, requester());
		ScreenTesting.waitForScreen(context, RedstoneRequesterScreen.class);
		context.takeScreenshot(shot("redstone_requester_opened"));

		// Two clicks, as a player makes them: the cobblestone up out of the inventory, then into the order.
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, Items.COBBLESTONE)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_ORDER_SLOT));

		ScreenTesting.click(context, ScreenTesting.widget(context, "addressBox"));
		context.getInput()
			.typeChars(ADDRESS);
		context.waitTicks(SETTLE_TICKS);

		// All or nothing, rather than taking whatever happens to be there.
		ScreenTesting.click(context, ScreenTesting.widget(context, "dontAllowPartial"));
		context.takeScreenshot(shot("redstone_requester_ordered"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SEND_TICKS);

		String addressOnBlock = server.computeOnServer(
			minecraftServer -> theRequester(minecraftServer).encodedTargetAdress);
		boolean partial = server.computeOnServer(
			minecraftServer -> theRequester(minecraftServer).allowPartialRequests);
		boolean ordersCobblestone = server.computeOnServer(minecraftServer -> theRequester(minecraftServer).encodedRequest
			.stacks()
			.stream()
			.anyMatch(big -> big.stack.is(Items.COBBLESTONE)));

		assertEquals(ADDRESS, addressOnBlock, "The address typed onto the requester did not reach the block");
		assertTrue(ordersCobblestone, "The item put into the order did not reach the block");
		assertEquals(false, partial, "Choosing to refuse partial requests did not reach the block");
	}

	private RedstoneRequesterBlockEntity theRequester(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(requester());

		if (!(be instanceof RedstoneRequesterBlockEntity requesterBe))
			throw new AssertionError("There is no redstone requester at " + requester() + " but " + be);

		return requesterBe;
	}

	private BlockPos requester() {
		return new BlockPos(60, -58, 60);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
