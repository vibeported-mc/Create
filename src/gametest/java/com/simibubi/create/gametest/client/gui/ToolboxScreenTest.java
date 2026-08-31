package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxScreen;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The toolbox, which is eight compartments a player drops tools into.
 * <p>
 * A toolbox opens on a click that is not crouching - crouching at one picks it up instead - and the
 * compartments are its own slots, laid out before the player's inventory is added after them. So the
 * first compartment is the first slot on the screen, and a pickaxe carried into it should be the pickaxe
 * the block is left holding.
 * <p>
 * The other way in is not a click at all: a key brings up a ring of whichever toolboxes are near enough
 * to reach, which is how a player takes a tool out without walking over to the box.
 */
@SharedWorld
public class ToolboxScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** The first compartment, which the menu lays out before the player's own inventory. */
	private static final int FIRST_COMPARTMENT = 0;

	private static final Item TOOL = Items.DIAMOND_PICKAXE;

	@ClientGameTest(screenshot = false)
	@DisplayName("A tool put into a toolbox compartment is the one the toolbox is left holding")
	void takesAToolIntoACompartment(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, toolbox(), 4);
		server.runCommand("setblock %d %d %d create:red_toolbox[facing=south]".formatted(toolbox().getX(),
			toolbox().getY(), toolbox().getZ()));
		server.runCommand("item replace entity @a hotbar.0 with minecraft:diamond_pickaxe");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, toolbox());
		ScreenTesting.waitForScreen(context, ToolboxScreen.class);
		context.takeScreenshot(shot("toolbox_opened"));

		// Two clicks, as a player makes them: the pickaxe up out of the inventory, then into the toolbox.
		ScreenTesting.click(context, ScreenTesting.slot(context, ScreenTesting.slotHolding(context, TOOL)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_COMPARTMENT));
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("toolbox_filled"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);

		assertEquals(TOOL, inTheToolbox(server), "The tool put into the first compartment did not reach the block");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("The toolbox key brings up the ring of nearby toolboxes")
	void opensTheRingOfNearbyToolboxes(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, toolbox(), 4);
		server.runCommand("setblock %d %d %d create:red_toolbox[facing=south]".formatted(toolbox().getX(),
			toolbox().getY(), toolbox().getZ()));
		context.waitTicks(SETTLE_TICKS);

		// Nothing is clicked here. The ring is a key away, and what it offers is whichever toolboxes are
		// near enough to reach - so the player is stood in front of this one first.
		ScreenTesting.lookAtBlock(context, server, toolbox());
		context.getInput()
			.pressKey(GLFW.GLFW_KEY_LEFT_ALT);

		ScreenTesting.waitForScreen(context, RadialToolboxMenu.class);
		context.takeScreenshot(shot("toolbox_ring"));

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);
	}

	private Item inTheToolbox(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(toolbox());

			if (!(be instanceof ToolboxBlockEntity toolboxBe))
				throw new AssertionError("There is no toolbox at " + toolbox() + " but " + be);

			return ItemUtil.getStack(compartmentsOf(toolboxBe), FIRST_COMPARTMENT)
				.getItem();
		});
	}

	/** A toolbox keeps its compartments to itself, so they are read rather than asked for. */
	@SuppressWarnings("unchecked")
	private static ResourceHandler<ItemResource> compartmentsOf(ToolboxBlockEntity toolbox) {
		return (ResourceHandler<ItemResource>) ScreenTesting.read(toolbox, "inventory");
	}

	private BlockPos toolbox() {
		return new BlockPos(60, -58, 60);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
