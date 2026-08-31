package com.simibubi.create.content.logistics.depot;

import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a depot as a resource handler: index 0 is the held item, the rest are the processing
 * output buffer.
 * <p>
 * The held item lives on the behaviour rather than in a resource handler of its own, so it is made
 * transactional here with a {@link SnapshotJournal}: the held stack is copied before a transfer
 * touches it and restored if the transaction rolls back. The output buffer is already an
 * {@code ItemStacksResourceHandler} and handles its own transactions.
 */
public class DepotItemHandler implements ResourceHandler<ItemResource> {

	private static final int MAIN_SLOT = 0;
	private final DepotBehaviour behaviour;
	private final HeldItemJournal journal = new HeldItemJournal();

	public DepotItemHandler(DepotBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	@Override
	public int size() {
		return 9;
	}

	@Override
	public ItemResource getResource(int index) {
		return index == MAIN_SLOT ? ItemResource.of(behaviour.getHeldItemStack())
			: behaviour.processingOutputBuffer.getResource(index - 1);
	}

	@Override
	public long getAmountAsLong(int index) {
		return index == MAIN_SLOT ? behaviour.getHeldItemStack()
			.getCount() : behaviour.processingOutputBuffer.getAmountAsLong(index - 1);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return index == MAIN_SLOT ? behaviour.maxStackSize.get()
			: behaviour.processingOutputBuffer.getCapacityAsLong(index - 1, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return index == MAIN_SLOT && behaviour.isItemValid(resource.toStack(1));
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index != MAIN_SLOT || resource.isEmpty() || amount <= 0)
			return 0;
		if (!behaviour.getHeldItemStack()
			.isEmpty() && !behaviour.canMergeItems())
			return 0;
		if (!behaviour.isOutputEmpty() && !behaviour.canMergeItems())
			return 0;

		journal.updateSnapshots(transaction);
		ItemStack stack = resource.toStack(amount);
		ItemStack remainder = behaviour.insert(new TransportedItemStack(stack), false);
		return amount - remainder.getCount();
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;
		if (index != MAIN_SLOT)
			return behaviour.processingOutputBuffer.extract(index - 1, resource, amount, transaction);

		TransportedItemStack held = behaviour.heldItem;
		if (held == null || !resource.matches(held.stack))
			return 0;

		journal.updateSnapshots(transaction);
		int extracted = Math.min(amount, held.stack.getCount());
		ItemStack remaining = held.stack.copy();
		remaining.shrink(extracted);
		if (remaining.isEmpty())
			behaviour.heldItem = null;
		else
			behaviour.heldItem.stack = remaining;
		return extracted;
	}

	private class HeldItemJournal extends SnapshotJournal<@Nullable TransportedItemStack> {
		@Override
		protected @Nullable TransportedItemStack createSnapshot() {
			return behaviour.heldItem == null ? null : behaviour.heldItem.copy();
		}

		@Override
		protected void revertToSnapshot(@Nullable TransportedItemStack snapshot) {
			behaviour.heldItem = snapshot;
		}

		@Override
		protected void onRootCommit(@Nullable TransportedItemStack originalState) {
			behaviour.blockEntity.notifyUpdate();
		}
	}

}
