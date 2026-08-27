package com.simibubi.create.content.logistics.tunnel;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A brass tunnel either holds one stack back for distribution, or is a pass-through onto the belt
 * underneath it. The stack it holds lives on the block entity, so it is made transactional here
 * with a {@link SnapshotJournal}; the belt handles its own.
 */
public class BrassTunnelItemHandler implements ResourceHandler<ItemResource> {

	private final BrassTunnelBlockEntity blockEntity;
	private final DistributedStackJournal journal = new DistributedStackJournal();

	public BrassTunnelItemHandler(BrassTunnelBlockEntity be) {
		this.blockEntity = be;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(blockEntity.stackToDistribute);
	}

	@Override
	public long getAmountAsLong(int index) {
		return blockEntity.stackToDistribute.getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return blockEntity.stackToDistribute.isEmpty() ? 64 : blockEntity.stackToDistribute.getMaxStackSize();
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;

		if (!blockEntity.hasDistributionBehaviour()) {
			ResourceHandler<ItemResource> beltCapability = blockEntity.getBeltCapability();
			if (beltCapability == null)
				return 0;
			return beltCapability.insert(index, resource, amount, transaction);
		}

		if (!blockEntity.canTakeItems())
			return 0;

		int accepted = Math.min(amount, resource.getMaxStackSize());
		journal.updateSnapshots(transaction);
		blockEntity.setStackToDistribute(resource.toStack(accepted), null);
		return accepted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		ResourceHandler<ItemResource> beltCapability = blockEntity.getBeltCapability();
		if (beltCapability == null)
			return 0;
		return beltCapability.extract(index, resource, amount, transaction);
	}

	private class DistributedStackJournal extends SnapshotJournal<ItemStack> {
		@Override
		protected ItemStack createSnapshot() {
			return blockEntity.stackToDistribute.copy();
		}

		@Override
		protected void revertToSnapshot(ItemStack snapshot) {
			blockEntity.setStackToDistribute(snapshot, null);
		}

		@Override
		protected void onRootCommit(@Nullable ItemStack originalState) {
			blockEntity.notifyUpdate();
		}
	}

}
