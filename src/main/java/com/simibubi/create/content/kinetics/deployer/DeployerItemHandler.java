package com.simibubi.create.content.kinetics.deployer;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a deployer as a resource handler: the overflow items first, then the held item last.
 * <p>
 * Neither of those is a resource handler of its own - the held item lives on the fake player and the
 * overflow is a plain list - so both are made transactional here with a {@link SnapshotJournal}
 * that copies them before a transfer touches them.
 */
public class DeployerItemHandler implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {

	private final DeployerBlockEntity be;
	private final DeployerFakePlayer player;
	private final ContentsJournal journal = new ContentsJournal();

	public DeployerItemHandler(DeployerBlockEntity be) {
		this.be = be;
		this.player = be.player;
	}

	@Override
	public int size() {
		return 1 + be.overflowItems.size();
	}

	public ItemStack getHeld() {
		if (player == null)
			return ItemStack.EMPTY;
		return player.getMainHandItem();
	}

	public void set(ItemStack stack) {
		if (player == null)
			return;
		if (be.getLevel()
			.isClientSide())
			return;
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		be.setChanged();
		be.sendData();
	}

	private boolean isHeldSlot(int index) {
		return index >= be.overflowItems.size();
	}

	private ItemStack stackAt(int index) {
		return isHeldSlot(index) ? getHeld() : be.overflowItems.get(index);
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(stackAt(index));
	}

	@Override
	public long getAmountAsLong(int index) {
		return stackAt(index).getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return resource.isEmpty() ? stackAt(index).getMaxStackSize() : resource.getMaxStackSize();
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		FilteringBehaviour filteringBehaviour = be.getBehaviour(FilteringBehaviour.TYPE);
		return filteringBehaviour == null || filteringBehaviour.test(resource.toStack(1));
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;
		// Only the held slot accepts insertion; the overflow is an output.
		if (!isHeldSlot(index) || !isValid(index, resource))
			return 0;

		ItemStack held = getHeld();
		int inserted;
		if (held.isEmpty())
			inserted = Math.min(amount, resource.getMaxStackSize());
		else if (!resource.matches(held))
			return 0;
		else
			inserted = Math.min(amount, held.getMaxStackSize() - held.getCount());

		if (inserted <= 0)
			return 0;

		journal.updateSnapshots(transaction);
		set(resource.toStack(held.getCount() + inserted));
		return inserted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;

		if (!isHeldSlot(index)) {
			ItemStack overflow = be.overflowItems.get(index);
			if (!resource.matches(overflow))
				return 0;
			int extracted = Math.min(amount, overflow.getCount());
			journal.updateSnapshots(transaction);
			ItemStack remaining = overflow.copy();
			remaining.shrink(extracted);
			if (remaining.isEmpty())
				be.overflowItems.remove(index);
			else
				be.overflowItems.set(index, remaining);
			return extracted;
		}

		ItemStack held = getHeld();
		if (!resource.matches(held))
			return 0;
		// A filtered deployer holds onto what matches its filter.
		if (!be.filtering.getFilter()
			.isEmpty() && be.filtering.test(held))
			return 0;

		int extracted = Math.min(amount, held.getCount());
		journal.updateSnapshots(transaction);
		set(resource.toStack(held.getCount() - extracted));
		return extracted;
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		ItemStack stack = resource.toStack(amount);
		if (isHeldSlot(index))
			set(stack);
		else
			be.overflowItems.set(index, stack);
	}

	private class ContentsJournal extends SnapshotJournal<ContentsJournal.Snapshot> {
		private record Snapshot(ItemStack held, List<ItemStack> overflow) {
		}

		@Override
		protected Snapshot createSnapshot() {
			List<ItemStack> overflow = new ArrayList<>(be.overflowItems.size());
			for (ItemStack stack : be.overflowItems)
				overflow.add(stack.copy());
			return new Snapshot(getHeld().copy(), overflow);
		}

		@Override
		protected void revertToSnapshot(Snapshot snapshot) {
			set(snapshot.held());
			be.overflowItems.clear();
			be.overflowItems.addAll(snapshot.overflow());
		}

		@Override
		protected void onRootCommit(Snapshot originalState) {
			be.setChanged();
			be.sendData();
		}
	}

}
