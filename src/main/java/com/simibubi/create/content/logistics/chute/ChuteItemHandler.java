package com.simibubi.create.content.logistics.chute;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A chute holds a single stack directly on the block entity, so that field is what gets snapshotted
 * for transactions.
 */
public class ChuteItemHandler implements ResourceHandler<ItemResource> {

	private ChuteBlockEntity blockEntity;
	private final ItemJournal journal = new ItemJournal();

	public ChuteItemHandler(ChuteBlockEntity be) {
		this.blockEntity = be;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(blockEntity.item);
	}

	@Override
	public long getAmountAsLong(int index) {
		return blockEntity.item.getCount();
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
		if (resource.isEmpty() || amount <= 0)
			return 0;
		if (!blockEntity.item.isEmpty())
			return 0;
		ItemStack stack = resource.toStack(amount);
		if (!blockEntity.canAcceptItem(stack))
			return 0;

		int accepted = Math.min(amount, resource.getMaxStackSize());
		journal.updateSnapshots(transaction);
		blockEntity.setItem(resource.toStack(accepted));
		return accepted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0 || !resource.matches(blockEntity.item))
			return 0;

		int extracted = Math.min(amount, blockEntity.item.getCount());
		journal.updateSnapshots(transaction);
		ItemStack remainder = blockEntity.item.copy();
		remainder.shrink(extracted);
		blockEntity.setItem(remainder);
		return extracted;
	}

	private class ItemJournal extends SnapshotJournal<ItemStack> {
		@Override
		protected ItemStack createSnapshot() {
			return blockEntity.item.copy();
		}

		@Override
		protected void revertToSnapshot(ItemStack snapshot) {
			blockEntity.setItem(snapshot);
		}
	}

}
