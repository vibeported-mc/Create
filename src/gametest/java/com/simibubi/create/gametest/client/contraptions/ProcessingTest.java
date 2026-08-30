package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Machines that work what a belt brings them: a press standing over the belt, and a saw at the end of
 * one.
 * <p>
 * Both are built in the same place, one after the other, with the first taken down before the second
 * goes up - a machine only needs the belt that feeds it, and rebuilding in place keeps the test to one
 * world and one camera.
 * <p>
 * Ten of everything goes in, and the count that comes out is checked exactly. Every hand-off here runs
 * through the transfer API - chute into belt, belt into machine, machine into chute, chute into chest -
 * and the way that goes wrong in this port is by the item count drifting, so ten in has to be ten
 * worth out and nothing left behind.
 */
@SharedWorld
public class ProcessingTest {

	private static final int GROUND = -60;

	/** The belt everything rides, with room beneath it for what falls off the end. */
	private static final int BELT_Y = GROUND + 2;

	/** The lane the whole scene is built along. */
	private static final int LANE = 0;

	private static final int BELT_RPM = 32;
	private static final int PRESS_RPM = 64;
	private static final int SAW_RPM = 128;

	/** Enough of them that an item lost or conjured on the way shows up in the count. */
	private static final int COUNT = 10;

	private static final int PATIENCE_TICKS = 600;

	@ClientGameTest(screenshot = false)
	@DisplayName("A press works the iron the belt brings it")
	void pressesWhatTheBeltBrings(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();
		watchTheLane(context, server);

		clearTheLane(server);
		buildPress(server);
		context.showContainerOverlay(source());
		context.showContainerOverlay(pressOutput());
		context.waitTicks(20);
		context.takeScreenshot(shot("press_before"));

		String pressed = waitFor(context, server, pressOutput(), "create:iron_sheet " + COUNT);
		context.takeScreenshot(shot("press_after"));
		context.clearOverlays();

		assertEquals("create:iron_sheet " + COUNT, pressed,
			"The press did not turn every iron ingot the belt brought it into a sheet");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("A saw cuts the stone the belt brings it")
	void sawsWhatTheBeltBrings(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();
		watchTheLane(context, server);

		clearTheLane(server);
		buildSaw(server);
		context.showContainerOverlay(source());
		context.showContainerOverlay(sawOutput());
		context.waitTicks(20);
		context.takeScreenshot(shot("saw_before"));

		String cut = waitFor(context, server, sawOutput(), "minecraft:stone_slab " + COUNT * 2);
		context.takeScreenshot(shot("saw_after"));
		context.clearOverlays();

		assertEquals("minecraft:stone_slab " + COUNT * 2, cut,
			"The saw did not cut every stone the belt brought it");
	}

	/**
	 * A belt with a press standing over the middle of it, each turned at its own speed.
	 * <p>
	 * The press looks for what to work on two blocks below itself, which is what puts it a block clear
	 * of the belt rather than resting on it.
	 */
	private void buildPress(TestServerContext server) {
		layBeltFrom(server, 0, 6);
		feed(server, "minecraft:iron_ingot");

		setBlock(server, new BlockPos(3, BELT_Y + 2, LANE), "create:mechanical_press[facing=east]");
		setBlock(server, new BlockPos(2, BELT_Y + 2, LANE), "create:creative_motor[facing=east]");

		// A funnel at the end takes what arrives one item at a time, and will not take another while the
		// chest below it is full - which is what stops a belt rather than spilling off the end of it.
		setBlock(server, new BlockPos(7, BELT_Y, LANE), "create:andesite_funnel[facing=up]");
		setBlock(server, pressOutput(), "minecraft:chest");

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			connectBelt(level, 0, 6);
			turn(level, new BlockPos(2, BELT_Y + 2, LANE), PRESS_RPM);
		});

		loadOntoBelt(server);
	}

	/**
	 * A belt running into a saw, which cuts what arrives and hands it on to the chute beyond it.
	 * <p>
	 * A saw only works on what it is given when it faces up, and it passes what it has cut to whatever
	 * stands in the direction the item was already travelling.
	 */
	private void buildSaw(TestServerContext server) {
		layBeltFrom(server, 0, 4);
		feed(server, "minecraft:stone");

		// The blade has to run along the belt, not across it, or the saw hands what it cuts off sideways
		// into nothing. The property reads the other way round to its name - the saw takes its item
		// movement from `!axis_along_first` - so along the x the belt runs on is the false one.
		setBlock(server, new BlockPos(5, BELT_Y, LANE),
			"create:mechanical_saw[facing=up,axis_along_first=false]");
		// A saw facing up turns on the axis across its blade, so it is driven from the side rather than
		// from underneath.
		setBlock(server, sawMotor(), "create:creative_motor[facing=south]");

		setBlock(server, new BlockPos(6, BELT_Y, LANE), "create:andesite_funnel[facing=up]");
		setBlock(server, sawOutput(), "minecraft:chest");

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			connectBelt(level, 0, 4);
			turn(level, sawMotor(), SAW_RPM);

			// Stone can be cut a dozen ways, so a saw left to itself has no reason to prefer one. The
			// filter is how a player says which, and without it the saw holds on to what it is given.
			FilteringBehaviour filter =
				BlockEntityBehaviour.get(level, new BlockPos(5, BELT_Y, LANE), FilteringBehaviour.TYPE);

			if (filter == null)
				throw new AssertionError("The saw has no filter to set");

			filter.setFilter(new ItemStack(Items.STONE_SLAB));
		});

		loadOntoBelt(server);
	}

	/** The pulleys a belt runs between, and the motor that turns them. */
	private void layBeltFrom(TestServerContext server, int from, int to) {
		setBlock(server, new BlockPos(from, BELT_Y, LANE), "create:shaft[axis=z]");
		setBlock(server, new BlockPos(to, BELT_Y, LANE), "create:shaft[axis=z]");
		setBlock(server, beltMotor(), "create:creative_motor[facing=south]");
	}

	/** The chest the run starts from, standing beside the belt rather than over it. */
	private void feed(TestServerContext server, String item) {
		setBlock(server, source(), "minecraft:chest");
		server.runCommand("item replace block %d %d %d container.0 with %s %d".formatted(source().getX(),
			source().getY(), source().getZ(), item, COUNT));
	}

	/**
	 * The funnel that empties the chest onto the belt, one item at a time.
	 * <p>
	 * It stands on the belt with the chest beside it, and it only counts as a belt funnel at all while
	 * there is a belt underneath - so it goes down after the belt, not before.
	 */
	private void loadOntoBelt(TestServerContext server) {
		setBlock(server, new BlockPos(0, BELT_Y + 1, LANE),
			"create:andesite_belt_funnel[facing=south,shape=pushing,powered=false]");
	}

	private void connectBelt(ServerLevel level, int from, int to) {
		BeltConnectorItem.createBelts(level, new BlockPos(from, BELT_Y, LANE), new BlockPos(to, BELT_Y, LANE));

		// Away from the chute that feeds it, so what lands travels the length of the belt.
		turn(level, beltMotor(), -BELT_RPM);
	}

	private void turn(ServerLevel level, BlockPos motor, int rpm) {
		((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(rpm);
	}

	private BlockPos sawMotor() {
		return new BlockPos(5, BELT_Y, LANE - 1);
	}

	private BlockPos beltMotor() {
		return new BlockPos(0, BELT_Y, LANE - 1);
	}

	private BlockPos source() {
		return new BlockPos(0, BELT_Y + 1, LANE - 1);
	}

	private BlockPos pressOutput() {
		return new BlockPos(7, BELT_Y - 1, LANE);
	}

	private BlockPos sawOutput() {
		return new BlockPos(6, BELT_Y - 1, LANE);
	}

	/** Takes the whole scene down, so the next machine is built on bare ground. */
	private void clearTheLane(TestServerContext server) {
		server.runCommand("fill %d %d %d %d %d %d air".formatted(-2, BELT_Y - 3, LANE - 2, 8, BELT_Y + 4,
			LANE + 2));
		server.runCommand("kill @e[type=item]");
	}

	/**
	 * Waits for a chest to hold what is expected of it, and reports what it holds either way.
	 * <p>
	 * Waiting for the chest the items came from to empty would not do: a chute drains a chest in a
	 * second, long before what it dropped has ridden the belt and been worked on.
	 */
	private String waitFor(ClientGameTestContext context, TestServerContext server, BlockPos chest,
		String expected) {
		int waited = 0;
		String holding = contentsOf(server, chest);

		while (waited < PATIENCE_TICKS && !holding.equals(expected)) {
			context.waitTicks(20);
			waited += 20;
			holding = contentsOf(server, chest);
		}

		// A moment more, so that anything still on its way would show up as a count that is too high
		// rather than passing on its way past the right answer.
		context.waitTicks(40);
		return contentsOf(server, chest);
	}

	/** What a chest holds, as one line of item and count, which is what the test asserts against. */
	private String contentsOf(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(pos) instanceof Container container))
				return "";

			Map<String, Integer> held = new LinkedHashMap<>();

			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (!stack.isEmpty())
					held.merge(BuiltInRegistries.ITEM.getKey(stack.getItem())
						.toString(), stack.getCount(), Integer::sum);
			}

			StringBuilder listing = new StringBuilder();
			held.forEach((item, count) -> listing.append(listing.isEmpty() ? "" : ", ")
				.append(item)
				.append(' ')
				.append(count));

			return listing.toString();
		});
	}

	/** Along the lane from one side, so the press standing over the belt is in view. */
	private static void watchTheLane(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 3.0 %d 11.0 facing 3.0 %d 0.0".formatted(BELT_Y + 4, BELT_Y));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
