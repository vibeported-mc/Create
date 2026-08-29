package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The ways Create moves items about, four lanes side by side: a chute, a smart chute that passes only
 * what its filter allows, a belt loaded and unloaded by andesite funnels, and the same belt again with
 * brass funnels and a filter on the one doing the loading.
 * <p>
 * Three kinds of item travel every lane, so a lane that carries one kind and mangles another cannot
 * hide. Where a filter is involved the test asks two questions rather than one: did the allowed kind
 * get through, and is the refused kind still where it started.
 */
public class ItemLogisticsTest {

	private static final int GROUND = -60;

	/** Three different things down every lane. */
	private static final List<Item> CARGO = List.of(Items.GRAVEL, Items.COBBLESTONE, Items.COPPER_INGOT);

	/** What the filters are set to; the other two kinds should be turned away. */
	private static final Item ALLOWED = Items.COPPER_INGOT;

	private static final int COUNT = 32;

	private static final int PATIENCE_TICKS = 1200;

	/** Every lane is a belt, so they all run at the same height. */
	private static final int BELT_Y = GROUND + 2;

	/** The four lanes stand in a row, a few blocks apart, so one shot takes all of them in. */
	private static final int CHUTE_LANE = 0;
	private static final int SMART_CHUTE_LANE = 4;
	private static final int ANDESITE_LANE = 8;
	private static final int BRASS_LANE = 12;

	private static final List<Integer> LANES = List.of(CHUTE_LANE, SMART_CHUTE_LANE, ANDESITE_LANE, BRASS_LANE);

	/** How far a belt runs, from its loading end to its unloading end. */
	private static final int BELT_RUN = 4;

	@ClientGameTest(screenshot = false)
	@DisplayName("Chutes, belts and belt funnels all carry items")
	void everythingCarries(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		for (int lane : LANES)
			buildBelt(server, lane);

		buildChuteEnds(server, CHUTE_LANE, "create:chute[facing=down,shape=normal]");
		buildChuteEnds(server, SMART_CHUTE_LANE, "create:smart_chute");
		buildFunnelChests(server, ANDESITE_LANE);
		buildFunnelChests(server, BRASS_LANE);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			for (int lane : LANES)
				layBelt(level, lane);
			setFilter(level, chutePos(SMART_CHUTE_LANE), new ItemStack(ALLOWED));
		});

		// Funnels only stay funnels while there is a belt under them, so they go down after it.
		addFunnels(server, ANDESITE_LANE, "andesite");
		addFunnels(server, BRASS_LANE, "brass");

		server.runOnServer(minecraftServer -> setFilter(minecraftServer.overworld(), loadingFunnel(BRASS_LANE),
			new ItemStack(ALLOWED)));

		watchTheWholeThing(context, server);
		context.waitTicks(20);
		context.takeScreenshot(shot("item_logistics_before"));

		int waited = 0;
		while (waited < PATIENCE_TICKS && !everythingArrived(server)) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("item_logistics_after"));

		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		// Neither the plain chute nor the andesite funnels take a view on what they are carrying.
		for (Item item : CARGO) {
			assertEquals(COUNT, countIn(server, chuteDestination(CHUTE_LANE), item),
				"The chute did not pass all of the " + name(item));
			assertEquals(0, countIn(server, chuteSource(CHUTE_LANE), item),
				"The chute left some of the " + name(item) + " behind");
			assertEquals(COUNT, countIn(server, unloadingChest(ANDESITE_LANE), item),
				"The andesite funnels did not carry all of the " + name(item) + " along the belt");
		}

		// The two that filter should have taken the copper and left the rest where it was.
		assertEquals(COUNT, countIn(server, chuteDestination(SMART_CHUTE_LANE), ALLOWED),
			"The smart chute did not pass what its filter allows");
		assertEquals(COUNT, countIn(server, unloadingChest(BRASS_LANE), ALLOWED),
			"The brass funnel did not put what its filter allows onto the belt");

		for (Item item : CARGO) {
			if (item == ALLOWED)
				continue;

			assertEquals(0, countIn(server, chuteDestination(SMART_CHUTE_LANE), item),
				"The smart chute passed " + name(item) + ", which its filter should have turned away");
			assertEquals(COUNT, countIn(server, chuteSource(SMART_CHUTE_LANE), item),
				"The smart chute lost the " + name(item) + " it refused");
			assertEquals(0, countIn(server, unloadingChest(BRASS_LANE), item),
				"The brass funnel passed " + name(item) + ", which its filter should have turned away");
			assertEquals(COUNT, countIn(server, loadingChest(BRASS_LANE), item),
				"The brass funnel lost the " + name(item) + " it refused");
		}
	}

	private boolean everythingArrived(TestServerContext server) {
		for (Item item : CARGO) {
			if (countIn(server, chuteDestination(CHUTE_LANE), item) < COUNT)
				return false;
			if (countIn(server, unloadingChest(ANDESITE_LANE), item) < COUNT)
				return false;
		}

		return countIn(server, chuteDestination(SMART_CHUTE_LANE), ALLOWED) >= COUNT
			&& countIn(server, unloadingChest(BRASS_LANE), ALLOWED) >= COUNT;
	}

	/** The pulleys a belt runs between, and the motor that turns them. */
	private void buildBelt(TestServerContext server, int z) {
		setBlock(server, beltPos(z, 0), "create:shaft[axis=z]");
		setBlock(server, beltPos(z, BELT_RUN), "create:shaft[axis=z]");

		// A belt turns about the axis across it, so the motor stands beside the far pulley.
		setBlock(server, beltPos(z, BELT_RUN).south(), "create:creative_motor[facing=north]");
	}

	/**
	 * A chest emptying through a chute onto the near end of the belt, and a chute under the far end
	 * catching what rides off it into a second chest.
	 */
	private void buildChuteEnds(TestServerContext server, int z, String chute) {
		setBlock(server, chutePos(z), chute);
		setBlock(server, chuteSource(z), "minecraft:chest");
		setBlock(server, new BlockPos(BELT_RUN, BELT_Y - 1, z), "create:chute[facing=down,shape=normal]");
		setBlock(server, chuteDestination(z), "minecraft:chest");

		load(server, chuteSource(z));
	}

	/** The chests the two funnels of a lane serve. */
	private void buildFunnelChests(TestServerContext server, int z) {
		setBlock(server, loadingChest(z), "minecraft:chest");
		setBlock(server, unloadingChest(z), "minecraft:chest");
		load(server, loadingChest(z));
	}

	private BlockPos chuteDestination(int z) {
		return new BlockPos(BELT_RUN, BELT_Y - 2, z);
	}

	private BlockPos chutePos(int z) {
		return new BlockPos(0, BELT_Y + 1, z);
	}

	private BlockPos chuteSource(int z) {
		return new BlockPos(0, BELT_Y + 2, z);
	}

	/**
	 * The funnel that empties the first chest onto the belt, and the one that takes what arrives off it
	 * again. Both face south, which puts the chest each one serves on its north side.
	 */
	private void addFunnels(TestServerContext server, int z, String metal) {
		setBlock(server, loadingFunnel(z),
			"create:%s_belt_funnel[facing=south,shape=pushing,powered=false]".formatted(metal));
		setBlock(server, unloadingFunnel(z),
			"create:%s_belt_funnel[facing=south,shape=pulling,powered=false]".formatted(metal));
	}

	private BlockPos beltPos(int z, int along) {
		return new BlockPos(along, BELT_Y, z);
	}

	private BlockPos loadingFunnel(int z) {
		return beltPos(z, 0).above();
	}

	private BlockPos unloadingFunnel(int z) {
		return beltPos(z, BELT_RUN).above();
	}

	private BlockPos loadingChest(int z) {
		return loadingFunnel(z).north();
	}

	private BlockPos unloadingChest(int z) {
		return unloadingFunnel(z).north();
	}

	private void layBelt(ServerLevel level, int z) {
		BeltConnectorItem.createBelts(level, beltPos(z, 0), beltPos(z, BELT_RUN));
		((CreativeMotorBlockEntity) level.getBlockEntity(beltPos(z, BELT_RUN).south())).generatedSpeed
			.setValue(64);
	}

	private static void setFilter(ServerLevel level, BlockPos pos, ItemStack filter) {
		FilteringBehaviour filtering = BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE);

		if (filtering == null)
			throw new AssertionError("No filter to set at " + pos);

		filtering.setFilter(filter);
	}

	private void load(TestServerContext server, BlockPos chest) {
		for (int slot = 0; slot < CARGO.size(); slot++)
			give(server, chest, slot, name(CARGO.get(slot)), COUNT);
	}

	private int countIn(TestServerContext server, BlockPos pos, Item item) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(pos) instanceof Container container))
				return 0;

			int found = 0;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(item))
					found += stack.getCount();
			}
			return found;
		});
	}

	private static String name(Item item) {
		return BuiltInRegistries.ITEM.getKey(item)
			.toString();
	}

	/**
	 * Straight down over the middle of the row: each belt runs across the picture and the four of them
	 * stack up it, so nothing stands in front of anything else and every belt is in full view.
	 */
	private static void watchTheWholeThing(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 2.0 %d 6.0 0.0 90.0".formatted(BELT_Y + 10));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void give(TestServerContext server, BlockPos pos, int slot, String item, int count) {
		server.runCommand("item replace block %d %d %d container.%d with %s %d".formatted(pos.getX(), pos.getY(),
			pos.getZ(), slot, item, count));
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
