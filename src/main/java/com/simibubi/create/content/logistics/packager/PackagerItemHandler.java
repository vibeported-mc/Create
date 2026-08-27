package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ModifiableItemHandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the packager's single held box.
 * <p>
 * The box lives on the block entity, so it is made transactional here with a
 * {@link SnapshotJournal}. Insertion also unwraps the package into the attached inventory, which is
 * itself transactional, so both roll back together.
 */
public class PackagerItemHandler implements ModifiableItemHandler {

	private final PackagerBlockEntity blockEntity;
	private final HeldBoxJournal journal = new HeldBoxJournal();

	public PackagerItemHandler(PackagerBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(blockEntity.heldBox);
	}

	@Override
	public long getAmountAsLong(int index) {
		return blockEntity.heldBox.getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return 1;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return PackageItem.isPackage(resource.toStack(1));
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		if (index != 0)
			return;
		blockEntity.heldBox = resource.toStack(amount);
		blockEntity.notifyUpdate();
	}

	/**
	 * A packager takes one box at a time and unwraps it straight away rather than storing it.
	 */
	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index != 0 || resource.isEmpty() || amount <= 0)
			return 0;
		if (!blockEntity.heldBox.isEmpty() || !blockEntity.queuedExitingPackages.isEmpty())
			return 0;
		if (!isValid(index, resource))
			return 0;

		ItemStack box = resource.toStack(1);
		if (!blockEntity.unwrapBox(box, true))
			return 0;

		journal.updateSnapshots(transaction);
		blockEntity.unwrapBox(box, false);
		return 1;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index != 0 || resource.isEmpty() || amount <= 0)
			return 0;
		if (blockEntity.animationTicks != 0)
			return 0;
		if (!resource.matches(blockEntity.heldBox))
			return 0;

		journal.updateSnapshots(transaction);
		int extracted = Math.min(amount, blockEntity.heldBox.getCount());
		ItemStack remaining = blockEntity.heldBox.copy();
		remaining.shrink(extracted);
		blockEntity.heldBox = remaining;
		return extracted;
	}

	private class HeldBoxJournal extends SnapshotJournal<ItemStack> {
		@Override
		protected ItemStack createSnapshot() {
			return blockEntity.heldBox.copy();
		}

		@Override
		protected void revertToSnapshot(ItemStack snapshot) {
			blockEntity.heldBox = snapshot;
		}

		@Override
		protected void onRootCommit(ItemStack originalState) {
			blockEntity.triggerStockCheck();
			blockEntity.notifyUpdate();
		}
	}

}
