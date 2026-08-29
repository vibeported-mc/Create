package com.simibubi.create.content.kinetics.belt.transport;

import java.util.List;

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
 * The items belong to the belt inventory, and neither operation here touches it the way a plain slot
 * would: an insert is queued for the belt to take up on its next tick, and an extraction shrinks the
 * stack where it lies instead of lifting it off. So the snapshot holds what a rollback needs to undo
 * either - which item was at this offset and how much of it there was, and how long the belt queue
 * already was.
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

	/**
	 * What this segment can disturb, as it stood when a transaction first reached it.
	 *
	 * @param onBelt the item at this offset, which is the one an extraction goes on to shrink
	 * @param count  how much of it there was
	 * @param queued how many items the belt already had waiting to be let on
	 */
	private record Segment(@Nullable TransportedItemStack onBelt, int count, int queued) {
	}

	private class SegmentJournal extends SnapshotJournal<Segment> {
		@Override
		protected Segment createSnapshot() {
			TransportedItemStack transported = beltInventory.getStackAtOffset(offset);
			return new Segment(transported, transported == null ? 0 : transported.stack.getCount(),
				beltInventory.toInsert.size());
		}

		@Override
		protected void revertToSnapshot(Segment snapshot) {
			// Anything queued since was conjured by a transaction that only meant to ask. The belt does
			// not count it among its items yet, so a rollback that went looking there would miss it and
			// let it ride away as though the insert had stood.
			List<TransportedItemStack> queue = beltInventory.toInsert;
			if (queue.size() > snapshot.queued())
				queue.subList(snapshot.queued(), queue.size())
					.clear();

			// An extraction leaves the stack where it lies, so giving back what it took is a matter of
			// the count - and of sparing the stack the removal an emptied one is marked for.
			if (snapshot.onBelt() != null) {
				snapshot.onBelt().stack.setCount(snapshot.count());
				beltInventory.toRemove.remove(snapshot.onBelt());
			}
		}

		@Override
		protected void onRootCommit(Segment originalState) {
			beltInventory.belt.notifyUpdate();
		}
	}


}
