package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.table.SchematicTableBlockEntity;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The schematic table, which is a menu over a block rather than over an item in hand.
 * <p>
 * Nothing is uploaded here - that needs a file on disk, and what the upload does is not the table's own
 * doing. What is worth checking is the way in: the table opens on an empty-handed click, and the slot it
 * offers takes a blank schematic out of the player's inventory and holds onto it.
 * <p>
 * So the blank is carried into the slot with the cursor, the way a player puts one there, and the block
 * on the server is then asked whether it is really holding it.
 */
@SharedWorld
public class SchematicTableScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** The table's own first slot, before the player's inventory is laid out after it. */
	private static final int INPUT_SLOT = 0;

	@ClientGameTest(screenshot = false)
	@DisplayName("A blank schematic put into the table's slot is the one the table ends up holding")
	void takesABlankSchematic(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, table(), 4);
		server.runCommand("setblock %d %d %d create:schematic_table[facing=south]".formatted(table().getX(),
			table().getY(), table().getZ()));
		server.runCommand("item replace entity @a hotbar.0 with create:empty_schematic");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, table());
		ScreenTesting.waitForScreen(context, SchematicTableScreen.class);
		context.takeScreenshot(shot("schematic_table_opened"));

		// Two clicks, as a player makes them: the blank up out of the inventory, then down into the slot.
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, AllItems.EMPTY_SCHEMATIC.get())));
		ScreenTesting.click(context, ScreenTesting.slot(context, INPUT_SLOT));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("schematic_table_loaded"));

		boolean holdsIt = server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(table());

			if (!(be instanceof SchematicTableBlockEntity tableBe))
				throw new AssertionError("There is no schematic table at " + table() + " but " + be);

			ItemStack held = ItemUtil.getStack(tableBe.inventory, INPUT_SLOT);

			return AllItems.EMPTY_SCHEMATIC.isIn(held);
		});

		assertTrue(holdsIt, "The blank schematic put into the table's slot did not reach the table");
	}

	private BlockPos table() {
		return new BlockPos(60, -58, 60);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
