package com.simibubi.create.content.kinetics.belt.transport;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * One belt position exposed as a single-slot handler.
 * <p>
 * The item is owned by the belt inventory, so the snapshot copies the transported stack at this
 * offset and puts it back on rollback.
 */
public class ItemHandlerBeltSegment implements ResourceHandler<ItemResource> {

	private final BeltInventory beltInventory;
	int offset;
	private final SegmentJournal journal = new SegmentJournal();

	public ItemHandlerBeltSegment(BeltInventory beltInventory, int offset) {
		this.beltInventory = beltInventory;
		this.offset = offset;
	}

	private ItemStack stackAtOffset() {
		TransportedItemStack stackAtOffset = beltInventory.getStackAtOffset(offset);
		return stackAtOffset == null ? ItemStack.EMPTY : stackAtOffset.stack;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(stackAtOffset());
	}

	@Override
	public long getAmountAsLong(int index) {
		return stackAtOffset().getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return resource.isEmpty() ? 64 : resource.toStack(1)
			.getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0 || !beltInventory.canInsertAt(offset))
			return 0;

		int accepted = Math.min(amount, resource.getMaxStackSize());
		journal.updateSnapshots(transaction);

		TransportedItemStack newStack = new TransportedItemStack(resource.toStack(accepted));
		newStack.insertedAt = offset;
		newStack.beltPosition = offset + .5f + (beltInventory.beltMovementPositive ? -1 : 1) / 16f;
		newStack.prevBeltPosition = newStack.beltPosition;
		beltInventory.addItem(newStack);
		return accepted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransportedItemStack transported = beltInventory.getStackAtOffset(offset);
		if (transported == null || resource.isEmpty() || amount <= 0 || !resource.matches(transported.stack))
			return 0;

		int extracted = Math.min(amount, transported.stack.getCount());
		journal.updateSnapshots(transaction);
		transported.stack.shrink(extracted);
		if (transported.stack.isEmpty())
			beltInventory.toRemove.add(transported);
		return extracted;
	}

	private class SegmentJournal extends SnapshotJournal<@Nullable TransportedItemStack> {
		@Override
		protected @Nullable TransportedItemStack createSnapshot() {
			TransportedItemStack transported = beltInventory.getStackAtOffset(offset);
			return transported == null ? null : transported.copy();
		}

		@Override
		protected void revertToSnapshot(@Nullable TransportedItemStack snapshot) {
			TransportedItemStack current = beltInventory.getStackAtOffset(offset);
			if (current != null) {
				beltInventory.toRemove.remove(current);
				beltInventory.getTransportedItems()
					.remove(current);
			}
			if (snapshot != null)
				beltInventory.addItem(snapshot);
		}

		@Override
		protected void onRootCommit(@Nullable TransportedItemStack originalState) {
			beltInventory.belt.notifyUpdate();
		}
	}

}
