package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import com.simibubi.create.foundation.item.ContainerItemHandler;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import com.simibubi.create.foundation.transfer.Transactions;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTest;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Items that are moved from one place to another should still all be there afterwards.
 * <p>
 * These borrow a structure for its floor and build what they need on top of it.
 */
@GameTestGroup(path = "items")
public class TestTransferItems {

	/**
	 * A transaction that is not committed must leave a container as it found it.
	 * <p>
	 * There is no counterpart to this on 1.21.1: {@link ContainerItemHandler} is new in the port, so
	 * nothing can be compared against, and what it must obey is the transfer API's own rule - work done
	 * inside a transaction that is thrown away has to be thrown away with it. Both ways of writing to it
	 * are asked here, the transactional one and the direct one.
	 */
	@GameTest(template = "belt_coaster", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void containerHandlerHonoursRollback(CreateGameTestHelper helper) {
		BlockPos chestPos = new BlockPos(2, 2, 2);
		helper.setBlock(chestPos, Blocks.CHEST);

		helper.runAtTickTime(2, () -> {
			Container chest = (Container) helper.getLevel()
				.getBlockEntity(helper.absolutePos(chestPos));
			chest.setItem(0, new ItemStack(Items.DIAMOND, 10));

			ContainerItemHandler handler = new ContainerItemHandler(chest);

			// Inserting and then throwing the transaction away.
			try (Transaction transaction = Transactions.open()) {
				handler.insert(ItemResource.of(new ItemStack(Items.GOLD_INGOT)), 5, transaction);
			}

			expect(helper, chest, "after a rolled back insert");

			// Writing a slot outright and then throwing the transaction away.
			try (Transaction transaction = Transactions.open()) {
				handler.set(0, ItemResource.of(new ItemStack(Items.GOLD_INGOT)), 64);
			}

			expect(helper, chest, "after a rolled back slot write");

			helper.succeed();
		});
	}

	/**
	 * A vanilla container carried on a contraption has to arrive with what it left with.
	 * <p>
	 * This walks the path a chest takes when a contraption picks it up: its contents are copied into a
	 * mounted storage, the contraption is saved and read back, the storage is used while it travels, and
	 * on disassembly the contents are written into the block that gets placed. Each step is a place the
	 * port changed underneath - the copy on mounting reads through helpers that now hand back detached
	 * stacks, and the write on disassembly goes through {@link ContainerItemHandler#set}. Counting what
	 * goes in against what comes out catches a loss at any of them without having to guess which.
	 * <p>
	 * A moving contraption is not built here on purpose: the tests that do that already pass, and driving
	 * one adds timing to a question that is about storage rather than about movement.
	 */
	@GameTest(template = "belt_coaster", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void contraptionStorageRoundTrip(CreateGameTestHelper helper) {
		BlockPos chestPos = new BlockPos(2, 2, 2);
		BlockPos barrelPos = new BlockPos(3, 2, 2);
		helper.setBlock(chestPos, Blocks.CHEST);
		helper.setBlock(barrelPos, Blocks.BARREL);

		helper.runAtTickTime(2, () -> {
			roundTrip(helper, chestPos, "chest");
			roundTrip(helper, barrelPos, "barrel");
			helper.succeed();
		});
	}

	private static void roundTrip(CreateGameTestHelper helper, BlockPos pos, String what) {
		Level level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(pos);
		BlockState state = level.getBlockState(absolute);
		BlockEntity be = level.getBlockEntity(absolute);
		Container container = (Container) be;

		container.setItem(0, new ItemStack(Items.DIAMOND, 10));
		container.setItem(3, new ItemStack(Items.GOLD_INGOT, 7));

		// Assembly: the block's contents are taken into the contraption.
		MountedItemStorage mounted = AllMountedStorageTypes.SIMPLE.get()
			.mount(level, state, absolute, be);

		if (mounted == null)
			helper.fail("Nothing was mounted from the " + what);

		if (countOf(mounted, Items.DIAMOND) != 10 || countOf(mounted, Items.GOLD_INGOT) != 7)
			helper.fail("Mounting the " + what + " picked up " + countOf(mounted, Items.DIAMOND)
				+ " diamonds and " + countOf(mounted, Items.GOLD_INGOT) + " gold, not 10 and 7");

		// Saved and read back, as it would be when the contraption is written to disk.
		mounted = reserialize(helper, mounted, what);

		// Used while the contraption travels: some taken out, something new put in.
		ItemHandlerHelpers.extractItem(mounted, 0, 4, false);
		ItemHandlerHelpers.insertItemStacked(mounted, new ItemStack(Items.REDSTONE, 20), false);

		// Disassembly: the block is placed back empty and the contraption writes into it.
		container.clearContent();
		((SimpleMountedStorage) mounted).unmount(level, state, absolute, be);

		expect(helper, container, what, Items.DIAMOND, 6);
		expect(helper, container, what, Items.GOLD_INGOT, 7);
		expect(helper, container, what, Items.REDSTONE, 20);

		if (total(container) != 33)
			helper.fail("The " + what + " came back holding " + total(container) + " items, not the 33 put through it");
	}

	private static MountedItemStorage reserialize(CreateGameTestHelper helper, MountedItemStorage mounted,
		String what) {
		RegistryOps<Tag> ops = helper.getLevel()
			.registryAccess()
			.createSerializationContext(NbtOps.INSTANCE);

		Tag saved = MountedItemStorage.CODEC.encodeStart(ops, mounted)
			.getOrThrow(error -> new AssertionError("Saving the mounted " + what + " failed: " + error));

		return MountedItemStorage.CODEC.parse(ops, saved)
			.getOrThrow(error -> new AssertionError("Reading the mounted " + what + " back failed: " + error));
	}

	private static int countOf(MountedItemStorage storage, Item item) {
		int total = 0;
		for (int slot = 0; slot < storage.size(); slot++) {
			ItemStack held = ItemHandlerHelpers.getStackInSlot(storage, slot);
			if (held.is(item))
				total += held.getCount();
		}
		return total;
	}

	private static int total(Container container) {
		int total = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++)
			total += container.getItem(slot)
				.getCount();
		return total;
	}

	private static void expect(CreateGameTestHelper helper, Container container, String what, Item item,
		int expected) {
		int found = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++)
			if (container.getItem(slot)
				.is(item))
				found += container.getItem(slot)
					.getCount();

		if (found != expected)
			helper.fail("The " + what + " came back with " + found + " " + item + ", not the " + expected
				+ " it should have");
	}

	private static void expect(CreateGameTestHelper helper, Container chest, String when) {
		ItemStack held = chest.getItem(0);
		if (!held.is(Items.DIAMOND) || held.getCount() != 10)
			helper.fail("Chest holds " + held + " " + when + ", not the 10 diamonds it started with");

		for (int slot = 1; slot < chest.getContainerSize(); slot++)
			if (!chest.getItem(slot)
				.isEmpty())
				helper.fail("Chest gained " + chest.getItem(slot) + " in slot " + slot + " " + when);
	}
}
