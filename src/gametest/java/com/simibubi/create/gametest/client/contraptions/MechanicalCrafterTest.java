package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.gametest.client.gui.ScreenTesting;

import net.createmod.catnip.api.math.Pointing;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A wall of mechanical crafters making a pair of Crushing Wheels.
 * <p>
 * This is the one recipe shape that cannot be made any other way: five by five with the corners cut off,
 * sixteen andesite alloy around four planks and a stone in the middle. The wall of crafters has to be
 * built in that shape - a crafter left out of the pattern is a crafter that must not be there at all,
 * since a group only starts once every crafter in it is holding something.
 * <p>
 * Each crafter is filled the way a player fills one: an item in hand and a click on its face, one at a
 * time, twenty-one times. The last of those is what sets the whole wall going, and the result is pushed
 * down out of the bottom crafter into the chest below it.
 */
public class MechanicalCrafterTest {

	private static final int SETTLE_TICKS = 10;

	private static final int CRAFTER_RPM = 64;

	/** Long enough for the wall to pass everything inwards and assemble it. */
	private static final int PATIENCE_TICKS = 600;

	/**
	 * The Crushing Wheel's shape, as the recipe gives it.
	 * <p>
	 * A is andesite alloy, P a plank, S a stone, and a space is a corner where no crafter stands.
	 */
	private static final String[] PATTERN = {
		" AAA ",
		"AAPAA",
		"APSPA",
		"AAPAA",
		" AAA "
	};

	/** The wall faces south, so the player works on it and watches it from that side. */
	private static final Direction FACING = Direction.SOUTH;

	@ClientGameTest(screenshot = false)
	@DisplayName("A wall of mechanical crafters assembles a pair of crushing wheels")
	void assemblesACrushingWheel(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, output().below(), 10);
		build(server);
		context.waitTicks(SETTLE_TICKS);

		// Turning before it is filled, since a group only looks at itself once it is already running.
		server.runOnServer(minecraftServer -> ((CreativeMotorBlockEntity) minecraftServer.overworld()
			.getBlockEntity(motor())).generatedSpeed.setValue(CRAFTER_RPM));
		context.waitTicks(SETTLE_TICKS);

		context.takeScreenshot(shot("mechanical_crafter_empty"));

		fill(context, server);
		context.takeScreenshot(shot("mechanical_crafter_filled"));

		int waited = 0;

		while (waited < PATIENCE_TICKS && wheelsMade(server) == 0) {
			context.waitTicks(20);
			waited += 20;
		}

		watchTheWall(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("mechanical_crafter_done"));

		assertEquals(2, wheelsMade(server), "The wall did not put a pair of crushing wheels into the chest");
	}

	/** The wall itself, the chest under its bottom crafter, and the cogwheel that turns the lot. */
	private void build(TestServerContext server) {
		for (int row = 0; row < PATTERN.length; row++) {
			for (int column = 0; column < PATTERN[row].length(); column++) {
				if (PATTERN[row].charAt(column) == ' ')
					continue;

				BlockPos pos = cell(row, column);

				setBlock(server, pos, "create:mechanical_crafter[facing=%s,pointing=%s]"
					.formatted(FACING.getSerializedName(), pointingToward(passesTo(pos))));
			}
		}

		setBlock(server, output().below(), "minecraft:chest");

		// A crafter takes no shaft of its own, so the wall is turned by a cogwheel meshing with the one at
		// its edge, and that cogwheel is what the motor drives from behind.
		setBlock(server, cog(), "create:cogwheel[axis=z]");
		setBlock(server, motor(), "create:creative_motor[facing=north]");
	}

	/** One item into each crafter, in hand and clicked onto its face, the way a player fills a wall. */
	private void fill(ClientGameTestContext context, TestServerContext server) {
		for (int row = 0; row < PATTERN.length; row++) {
			for (int column = 0; column < PATTERN[row].length(); column++) {
				char ingredient = PATTERN[row].charAt(column);

				if (ingredient == ' ')
					continue;

				// Exactly one at a time: a crafter takes whatever is held, and a wall holding stacks would
				// set about making more than the pair being counted.
				server.runCommand("item replace entity @a hotbar.0 with %s 1".formatted(itemFor(ingredient)));
				context.runOnClient(client -> client.player.getInventory()
					.setSelectedSlot(0));

				// Long enough for the client to know it is holding the item before it clicks with it.
				context.waitTicks(SETTLE_TICKS);

				ScreenTesting.rightClickBlock(context, server, cell(row, column));

				// And long enough afterwards for the crafter to have taken it. The click reaches the server
				// a tick or two later, and handing the player the next item before then would mean this
				// crafter is filled with the one meant for the crafter after it.
				context.waitTicks(SETTLE_TICKS);
			}
		}
	}


	/**
	 * Which way a crafter hands its item on.
	 * <p>
	 * Everything falls down its own column; the bottom of each column then turns inwards towards the
	 * middle one, and the middle one hands down out of the wall, which is what makes it the way out.
	 */
	private Direction passesTo(BlockPos pos) {
		if (pos.equals(output()))
			return Direction.DOWN;

		if (hasCrafterAt(pos.below()))
			return Direction.DOWN;

		return pos.getX() < output().getX() ? Direction.EAST : Direction.WEST;
	}

	private boolean hasCrafterAt(BlockPos pos) {
		for (int row = 0; row < PATTERN.length; row++)
			for (int column = 0; column < PATTERN[row].length(); column++)
				if (PATTERN[row].charAt(column) != ' ' && cell(row, column).equals(pos))
					return true;

		return false;
	}

	/**
	 * Which of a crafter's four arrows points this way in the world.
	 * <p>
	 * An arrow is given as up, down, left or right of the crafter's own face rather than as a compass
	 * direction, so the four are tried and the one that comes out right is used.
	 */
	private static String pointingToward(Direction target) {
		for (Pointing pointing : Pointing.values()) {
			BlockState state = AllBlocks.MECHANICAL_CRAFTER.getDefaultState()
				.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, FACING)
				.setValue(MechanicalCrafterBlock.POINTING, pointing);

			if (MechanicalCrafterBlock.getTargetDirection(state) == target)
				return pointing.getSerializedName();
		}

		throw new AssertionError("No arrow on a " + FACING + " facing crafter points " + target);
	}

	private static String itemFor(char ingredient) {
		return switch (ingredient) {
			case 'A' -> "create:andesite_alloy";
			case 'P' -> "minecraft:oak_planks";
			case 'S' -> "minecraft:stone";
			default -> throw new AssertionError("The pattern asks for " + ingredient + ", which is nothing");
		};
	}

	/** Where a square of the pattern stands, with the first row at the top of the wall. */
	private BlockPos cell(int row, int column) {
		return new BlockPos(output().getX() - 2 + column, output().getY() + PATTERN.length - 1 - row,
			output().getZ());
	}

	/** The crafter at the bottom middle, which is the one that hands the finished wheels out. */
	private BlockPos output() {
		return new BlockPos(60, -57, 60);
	}

	/** Meshing with the crafter at the edge of the wall, level with its middle row. */
	private BlockPos cog() {
		return new BlockPos(57, -55, 60);
	}

	private BlockPos motor() {
		return cog().south();
	}

	/** How many crushing wheels ended up in the chest under the wall. */
	private int wheelsMade(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(output().below()) instanceof Container chest))
				return 0;

			int found = 0;

			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				ItemStack stack = chest.getItem(slot);

				if (AllBlocks.CRUSHING_WHEEL.isIn(stack))
					found += stack.getCount();
			}

			return found;
		});
	}

	/** Stands back far enough to see the whole wall. */
	private void watchTheWall(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		ScreenTesting.lookAt(context, server, Vec3.atCenterOf(cell(2, 2)),
			new Vec3(output().getX() + 0.5, output().getY() + 1, output().getZ() + 8));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
