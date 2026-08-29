package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity.SelectionMode;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A brass tunnel handing items between three belts running side by side, once for each way it can be
 * told to choose between them.
 * <p>
 * The belts are cased and carry a tunnel each, standing shoulder to shoulder so that the three of them
 * make one group. Items are fed onto the first belt and every belt ends in a chest, so that what each
 * mode did with them can be counted rather than guessed at.
 * <p>
 * Every mode is asked the same two questions. Did everything that went in come out again - a tunnel
 * that drops or conjures items fails whatever it does with the rest - and did the items end up spread
 * the way this mode says they should.
 */
public class BrassTunnelTest {

	private static final int GROUND = -60;

	/** All three belts of a scene run at this height. */
	private static final int BELT_Y = GROUND + 2;

	/** How far the belts run, with the tunnels standing partway along. */
	private static final int BELT_RUN = 4;
	private static final int TUNNEL_AT = 2;

	/** Three belts side by side, so their tunnels touch and make one group. */
	private static final int BELTS = 3;

	/** A count that divides evenly between the belts, so an even split is recognisable. */
	private static final int COUNT = 12;

	/**
	 * What the modes that divide a stack are given: one stack of something that stacks, which is the
	 * only thing there is to divide.
	 */
	private static final Item ONE_STACK = Items.COBBLESTONE;

	/**
	 * What the modes that choose between the belts are given: twelve things that cannot merge back into
	 * one on the way. Anything that stacks arrives as a single stack, and a single stack is a single
	 * choice however the mode makes it, which leaves turn-taking nothing to show for itself.
	 */
	private static final Item SEPARATE = Items.WOODEN_SWORD;

	/** One kind of item to each belt, for the tunnels told to sort rather than to share out. */
	private static final List<Item> SORTED = List.of(Items.DIAMOND, Items.IRON_INGOT, Items.COAL);

	private static final int EACH = 4;

	/** Long enough for this much to travel four blocks, and short enough to sit through. */
	private static final int PATIENCE_TICKS = 200;

	@ClientGameTest(screenshot = false)
	@DisplayName("Split shares what arrives out between the belts")
	void split(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.SPLIT, ONE_STACK, false);

		assertEquals(COUNT, total(arrived), "Split did not deliver everything that went in: " + arrived);
		assertDividedEvenly(arrived, "Split");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Forced split shares out between the belts as well")
	void forcedSplit(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.FORCED_SPLIT, ONE_STACK, false);

		assertEquals(COUNT, total(arrived), "Forced split did not deliver everything that went in: " + arrived);
		assertDividedEvenly(arrived, "Forced split");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Round robin takes the belts in turn")
	void roundRobin(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.ROUND_ROBIN, SEPARATE, false);

		assertEquals(COUNT, total(arrived), "Round robin did not deliver everything that went in: " + arrived);
		assertSpreadAcrossAll(arrived, "Round robin");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Forced round robin takes the belts in turn as well")
	void forcedRoundRobin(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.FORCED_ROUND_ROBIN, SEPARATE, false);

		assertEquals(COUNT, total(arrived),
			"Forced round robin did not deliver everything that went in: " + arrived);
		assertSpreadAcrossAll(arrived, "Forced round robin");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Prefer nearest keeps the items on the belt they came in on")
	void preferNearest(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.PREFER_NEAREST, SEPARATE, false);

		assertEquals(COUNT, total(arrived), "Prefer nearest did not deliver everything that went in: " + arrived);
		assertEquals(COUNT, arrived.get(0), "Prefer nearest sent items off the belt they arrived on: " + arrived);
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Randomize scatters the items but keeps them all")
	void randomize(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.RANDOMIZE, SEPARATE, false);

		assertEquals(COUNT, total(arrived), "Randomize did not deliver everything that went in: " + arrived);
		assertTrue(arrived.stream()
			.filter(count -> count > 0)
			.count() > 1, "Randomize sent every item the same way: " + arrived);
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Synchronize lets all three belts through together")
	void synchronize(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		// This one holds items back until every belt of the group has something waiting, so all three
		// are fed rather than just the first.
		List<Integer> arrived = run(context, singleplayer, server, SelectionMode.SYNCHRONIZE, SEPARATE, true);

		assertEquals(BELTS * COUNT, total(arrived),
			"Synchronize did not deliver everything that went in: " + arrived);
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Filtered tunnels send each kind of item down its own belt")
	void sortsByFilter(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		// Its own patch of the world, past the one each mode took.
		int origin = SelectionMode.values().length * 8;

		for (int belt = 0; belt < BELTS; belt++)
			buildBelt(server, origin + belt);

		// Everything goes in on the first belt, mixed together.
		setBlock(server, new BlockPos(0, BELT_Y + 1, origin), "create:chute[facing=down,shape=normal]");
		setBlock(server, source(origin), "minecraft:chest");

		for (int kind = 0; kind < SORTED.size(); kind++)
			give(server, source(origin), kind, SORTED.get(kind), EACH);

		setBlock(server, motor(origin), "create:creative_motor[facing=south]");

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			for (int belt = 0; belt < BELTS; belt++) {
				layBelt(level, origin + belt);
				encase(level, origin + belt);
			}

			((CreativeMotorBlockEntity) level.getBlockEntity(motor(origin))).generatedSpeed.setValue(-64);
		});

		for (int belt = 0; belt < BELTS; belt++) {
			setBlock(server, tunnel(origin + belt), "create:brass_tunnel");
			setBlock(server, unloader(origin + belt),
				"create:brass_belt_funnel[facing=west,shape=retracted,powered=false]");
		}

		// A tunnel is given its filters only once it has stood for a tick, so they are set after one.
		context.waitTicks(2);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			// A tunnel filters the side an item would leave by, which for these belts is the way they
			// run. One kind to each, so every item has exactly one way out of the group.
			for (int belt = 0; belt < BELTS; belt++)
				keepFor(level, tunnel(origin + belt), SORTED.get(belt));
		});

		lookDownOn(context, server, origin);

		for (int belt = 0; belt < BELTS; belt++)
			context.showContainerOverlay(destination(origin + belt));

		context.showContainerOverlay(source(origin));
		context.waitTicks(20);
		context.takeScreenshot(shot("sorted_before"));

		int waited = 0;

		while (waited < PATIENCE_TICKS && sortedSoFar(server, origin) < SORTED.size() * EACH) {
			context.waitTicks(10);
			waited += 10;
		}

		context.takeScreenshot(shot("sorted_after"));

		for (int belt = 0; belt < BELTS; belt++) {
			for (Item kind : SORTED) {
				int found = countIn(server, destination(origin + belt), kind);

				assertEquals(kind == SORTED.get(belt) ? EACH : 0, found,
					"Belt " + belt + " should have ended up with " + (kind == SORTED.get(belt) ? EACH : 0)
						+ " of " + kind + " and had " + found);
			}
		}
	}

	/** How much has reached the belt its filter sends it to. */
	private int sortedSoFar(TestServerContext server, int origin) {
		int found = 0;

		for (int belt = 0; belt < BELTS; belt++)
			found += countIn(server, destination(origin + belt), SORTED.get(belt));

		return found;
	}

	/** Tells a tunnel to let this one kind of item out of the side its belt runs towards. */
	private static void keepFor(ServerLevel level, BlockPos pos, Item kind) {
		if (!(BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE) instanceof
			SidedFilteringBehaviour filtering))
			throw new AssertionError("No tunnel to set the filter of at " + pos);

		filtering.setFilter(Direction.EAST, new ItemStack(kind));
	}

	/**
	 * Builds a scene of its own for the given mode, feeds it, and reports what reached the chest at the
	 * end of each belt.
	 *
	 * @param cargo         what to feed it with, which differs by what the mode does
	 * @param feedEveryBelt whether every belt is given a load, or only the first
	 */
	private List<Integer> run(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server, SelectionMode mode, Item cargo, boolean feedEveryBelt) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		int origin = originOf(mode);
		int sent = (feedEveryBelt ? BELTS : 1) * COUNT;

		for (int belt = 0; belt < BELTS; belt++) {
			buildBelt(server, origin + belt);
			if (belt == 0 || feedEveryBelt)
				feed(server, origin + belt, cargo);
		}

		// One motor drives the lot: the pulleys the belts end on stand in a row and pass the rotation
		// along between them.
		setBlock(server, motor(origin), "create:creative_motor[facing=south]");

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			for (int belt = 0; belt < BELTS; belt++) {
				layBelt(level, origin + belt);
				// A tunnel will not stand on a bare belt, only on a cased one, which is what putting one
				// down in play gives the belt anyway.
				encase(level, origin + belt);
			}
			// Away from the chutes that feed them, so what lands on a belt travels the length of it and
			// through the tunnel rather than straight off the near end.
			((CreativeMotorBlockEntity) level.getBlockEntity(motor(origin))).generatedSpeed.setValue(-64);
		});

		// Neither a tunnel nor a belt funnel stays what it is without a belt under it, so both go down
		// after the belts. The funnel faces back against the way the belt runs, which is what makes it
		// take from the belt rather than feed it.
		for (int belt = 0; belt < BELTS; belt++) {
			setBlock(server, tunnel(origin + belt), "create:brass_tunnel");
			setBlock(server, unloader(origin + belt),
				"create:brass_belt_funnel[facing=west,shape=retracted,powered=false]");
		}

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			for (int belt = 0; belt < BELTS; belt++)
				setMode(level, tunnel(origin + belt), mode);
		});

		lookDownOn(context, server, origin);

		// Every chest says what is in it, beside itself, so a picture of the scene shows where the items
		// went as well as where they are.
		for (int belt = 0; belt < BELTS; belt++) {
			if (belt == 0 || feedEveryBelt)
				context.showContainerOverlay(source(origin + belt));

			context.showContainerOverlay(destination(origin + belt));
		}

		context.waitTicks(20);
		context.takeScreenshot(shot(mode, "before"));

		int waited = 0;
		while (waited < PATIENCE_TICKS && total(collect(server, origin, cargo)) < sent) {
			context.waitTicks(10);
			waited += 10;
		}

		List<Integer> arrived = collect(server, origin, cargo);
		context.takeScreenshot(shot(mode, "after"));

		return arrived;
	}

	/** Each mode gets a patch of the world to itself, since they all share the one world. */
	private static int originOf(SelectionMode mode) {
		return mode.ordinal() * 8;
	}

	private void buildBelt(TestServerContext server, int z) {
		setBlock(server, beltPos(z, 0), "create:shaft[axis=z]");
		setBlock(server, beltPos(z, BELT_RUN), "create:shaft[axis=z]");

		// A belt drops what reaches its end on the floor unless something is there to take it, so a
		// funnel stands on the last belt block with the chest it fills behind it.
		setBlock(server, destination(z), "minecraft:chest");
	}

	/** A chest of items emptying through a chute onto the near end of a belt. */
	private void feed(TestServerContext server, int z, Item cargo) {
		setBlock(server, new BlockPos(0, BELT_Y + 1, z), "create:chute[facing=down,shape=normal]");
		setBlock(server, source(z), "minecraft:chest");
		give(server, source(z), cargo, COUNT);
	}

	private BlockPos beltPos(int z, int along) {
		return new BlockPos(along, BELT_Y, z);
	}

	private BlockPos motor(int origin) {
		return new BlockPos(0, BELT_Y, origin - 1);
	}

	private BlockPos tunnel(int z) {
		return new BlockPos(TUNNEL_AT, BELT_Y + 1, z);
	}

	private BlockPos source(int z) {
		return new BlockPos(0, BELT_Y + 2, z);
	}

	/** The funnel standing on the last belt block, taking what arrives into the chest behind it. */
	private BlockPos unloader(int z) {
		return new BlockPos(BELT_RUN, BELT_Y + 1, z);
	}

	private BlockPos destination(int z) {
		return new BlockPos(BELT_RUN + 1, BELT_Y + 1, z);
	}

	private void layBelt(ServerLevel level, int z) {
		BeltConnectorItem.createBelts(level, beltPos(z, 0), beltPos(z, BELT_RUN));
	}

	/** Brass casing on the belt segment the tunnel is to stand on. */
	private void encase(ServerLevel level, int z) {
		if (!(level.getBlockEntity(beltPos(z, TUNNEL_AT)) instanceof BeltBlockEntity belt))
			throw new AssertionError("No belt to encase at " + beltPos(z, TUNNEL_AT));

		belt.setCasingType(CasingType.BRASS);
	}

	private static void setMode(ServerLevel level, BlockPos pos, SelectionMode mode) {
		ScrollValueBehaviour selection = BlockEntityBehaviour.get(level, pos, ScrollValueBehaviour.TYPE);

		if (selection == null)
			throw new AssertionError(
				"No tunnel to set the mode of at " + pos + ", found " + level.getBlockState(pos));

		selection.setValue(mode.ordinal());
	}

	/** What reached the chest at the end of each belt, in the order the belts stand. */
	private List<Integer> collect(TestServerContext server, int origin, Item cargo) {
		List<Integer> counts = new ArrayList<>();

		for (int belt = 0; belt < BELTS; belt++)
			counts.add(countIn(server, destination(origin + belt), cargo));

		return counts;
	}

	private static int total(List<Integer> counts) {
		return counts.stream()
			.mapToInt(Integer::intValue)
			.sum();
	}

	/** A stack divided between the belts should land on them in equal parts. */
	private static void assertDividedEvenly(List<Integer> arrived, String mode) {
		for (int belt = 0; belt < BELTS; belt++)
			assertEquals(COUNT / BELTS, arrived.get(belt),
				mode + " did not divide the stack evenly between the belts: " + arrived);
	}

	private static void assertSpreadAcrossAll(List<Integer> arrived, String mode) {
		for (int belt = 0; belt < BELTS; belt++)
			assertTrue(arrived.get(belt) > 0, mode + " left belt " + belt + " with nothing: " + arrived);
	}

	private int countIn(TestServerContext server, BlockPos pos, Item cargo) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(pos) instanceof Container container))
				return 0;

			int found = 0;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(cargo))
					found += stack.getCount();
			}
			return found;
		});
	}

	/** Straight down over the middle of the three belts, so every one of them is in full view. */
	private static void lookDownOn(ClientGameTestContext context, TestServerContext server, int origin) {
		// The name of the running test is drawn over the whole gui rather than as part of the heads-up
		// display, so hiding the display still leaves a picture that says what it is of.
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 3.0 %d %d.0 0.0 90.0".formatted(BELT_Y + 6, origin + 1));
	}

	private static TestScreenshotOptions shot(SelectionMode mode, String when) {
		return shot(mode.name()
			.toLowerCase() + "_" + when);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of("brass_tunnel_" + name)
			.withSize(1280, 720);
	}

	/** A given kind into a given slot, for a chest that is to hold more than one kind. */
	private static void give(TestServerContext server, BlockPos pos, int slot, Item cargo, int count) {
		server.runCommand("item replace block %d %d %d container.%d with %s %d".formatted(pos.getX(),
			pos.getY(), pos.getZ(), slot, BuiltInRegistries.ITEM.getKey(cargo), count));
	}

	/** One heap of it where it stacks, a slot apiece where it does not. */
	private static void give(TestServerContext server, BlockPos pos, Item cargo, int count) {
		if (new ItemStack(cargo).getMaxStackSize() > 1) {
			server.runCommand("item replace block %d %d %d container.0 with %s %d".formatted(pos.getX(),
				pos.getY(), pos.getZ(), BuiltInRegistries.ITEM.getKey(cargo), count));
			return;
		}

		for (int slot = 0; slot < count; slot++)
			server.runCommand("item replace block %d %d %d container.%d with %s 1".formatted(pos.getX(),
				pos.getY(), pos.getZ(), slot, BuiltInRegistries.ITEM.getKey(cargo)));
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
