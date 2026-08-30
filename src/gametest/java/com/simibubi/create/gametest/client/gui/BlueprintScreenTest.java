package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import com.simibubi.create.foundation.item.ItemStackHandler;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The crafting blueprint, which is the only screen here that belongs to an entity rather than a block.
 * <p>
 * A blueprint is hung on a wall the way a picture is, so the test hangs one itself: the item goes into the
 * hand and the wall is clicked, which is what puts the entity there. Clicking the blueprint after that
 * reaches the entity rather than the wall behind it, since it stands flush against the face.
 * <p>
 * Its screen is a crafting grid of ghost slots. An item carried into the first of them should be the item
 * the blueprint is left remembering - and since the slots are the same kind the filters and the linked
 * controller use, this is the third place that behaviour is checked from.
 */
@SharedWorld
public class BlueprintScreenTest {

	private static final int SETTLE_TICKS = 10;

	/**
	 * The first square of the crafting grid.
	 * <p>
	 * The menu lays the player's own inventory out first - twenty seven and a hotbar of nine - and only
	 * then its own nine squares, the result and the icon.
	 */
	private static final int FIRST_GRID_SLOT = 36;

	private static final Item INGREDIENT = Items.COBBLESTONE;

	@ClientGameTest(screenshot = false)
	@DisplayName("An item put into a crafting blueprint's grid is the one the blueprint is left holding")
	void takesAnIngredient(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, wall(), 4);
		server.runCommand("setblock %d %d %d minecraft:stone".formatted(wall().getX(), wall().getY(),
			wall().getZ()));
		server.runCommand("item replace entity @a hotbar.0 with create:crafting_blueprint");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:cobblestone 1");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// Hung on the wall the way a player hangs one: the blueprint in hand, and the wall clicked.
		ScreenTesting.rightClickBlock(context, server, wall());
		context.waitTicks(SETTLE_TICKS);

		// Off the blueprints before clicking the one now hanging there, so a click that missed would put
		// nothing else on the wall.
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(1));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickEntityAt(context, server, blueprint());
		ScreenTesting.waitForScreen(context, BlueprintScreen.class);
		context.takeScreenshot(shot("blueprint_opened"));

		// Two clicks, as a player makes them: the cobblestone up out of the inventory, then into the grid.
		ScreenTesting.click(context, ScreenTesting.slot(context,
			ScreenTesting.slotHolding(context, INGREDIENT)));
		ScreenTesting.click(context, ScreenTesting.slot(context, FIRST_GRID_SLOT));
		context.takeScreenshot(shot("blueprint_filled"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "confirmButton"));
		ScreenTesting.waitForNoScreen(context);
		context.waitTicks(SETTLE_TICKS);

		assertEquals(INGREDIENT, inTheBlueprint(server),
			"The item put into the blueprint's grid did not reach the blueprint");
	}

	private Item inTheBlueprint(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			List<BlueprintEntity> hanging = minecraftServer.overworld()
				.getEntitiesOfClass(BlueprintEntity.class, new AABB(wall()).inflate(3));

			if (hanging.isEmpty())
				throw new AssertionError("No blueprint was hung on the wall at " + wall());

			// A blueprint keeps its sections to itself, so the one at the front is asked through its own
			// class rather than by name.
			Object section = ScreenTesting.invoke(hanging.get(0), "getSectionAt", Vec3.ZERO);
			ItemStackHandler items = (ItemStackHandler) ScreenTesting.invoke(section, "getItems");

			return ItemHandlerHelpers.getStackInSlot(items, 0)
				.getItem();
		});
	}

	private BlockPos wall() {
		return new BlockPos(60, -57, 60);
	}

	/** The space in front of the wall, where a blueprint clicked onto its south face ends up. */
	private BlockPos blueprint() {
		return wall().south();
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
