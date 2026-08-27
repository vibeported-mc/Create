package com.simibubi.create.content.fluids.drain;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes an item drain's single held item as a resource handler.
 * <p>
 * The held item lives on the block entity rather than in a resource handler of its own, so it is
 * made transactional here with a {@link SnapshotJournal}: the held stack is copied before a transfer
 * touches it and restored if the transaction rolls back.
 */
public class ItemDrainItemHandler implements ResourceHandler<ItemResource> {

	private final ItemDrainBlockEntity blockEntity;
	private final Direction side;
	private final HeldItemJournal journal = new HeldItemJournal();

	public ItemDrainItemHandler(ItemDrainBlockEntity be, Direction side) {
		this.blockEntity = be;
		this.side = side;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(blockEntity.getHeldItemStack());
	}

	@Override
	public long getAmountAsLong(int index) {
		return blockEntity.getHeldItemStack()
			.getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return 64;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;
		if (!blockEntity.getHeldItemStack()
			.isEmpty())
			return 0;

		// The drain works on one item at a time when the stack is something it can empty, so that the
		// rest of the stack stays with the inserter.
		int accepted = amount;
		if (amount > 1 && GenericItemEmptying.canItemBeEmptied(blockEntity.getLevel(), resource.toStack(amount)))
			accepted = 1;
		else
			accepted = Math.min(accepted, resource.getMaxStackSize());

		journal.updateSnapshots(transaction);
		TransportedItemStack heldItem = new TransportedItemStack(resource.toStack(accepted));
		heldItem.prevBeltPosition = 0;
		blockEntity.setHeldItem(heldItem, side.getOpposite());
		return accepted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;

		TransportedItemStack held = blockEntity.heldItem;
		if (held == null || !resource.matches(held.stack))
			return 0;

		journal.updateSnapshots(transaction);
		int extracted = Math.min(amount, held.stack.getCount());
		ItemStack remaining = held.stack.copy();
		remaining.shrink(extracted);
		if (remaining.isEmpty())
			blockEntity.heldItem = null;
		else
			blockEntity.heldItem.stack = remaining;
		return extracted;
	}

	private class HeldItemJournal extends SnapshotJournal<@Nullable TransportedItemStack> {
		@Override
		protected @Nullable TransportedItemStack createSnapshot() {
			return blockEntity.heldItem == null ? null : blockEntity.heldItem.copy();
		}

		@Override
		protected void revertToSnapshot(@Nullable TransportedItemStack snapshot) {
			blockEntity.heldItem = snapshot;
		}

		@Override
		protected void onRootCommit(@Nullable TransportedItemStack originalState) {
			blockEntity.notifyUpdate();
		}
	}

}
