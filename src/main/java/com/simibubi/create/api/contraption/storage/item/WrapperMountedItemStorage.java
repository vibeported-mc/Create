package com.simibubi.create.api.contraption.storage.item;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Partial implementation of a MountedItemStorage that wraps an item handler.
 */
public abstract class WrapperMountedItemStorage<T extends ItemStacksResourceHandler> extends MountedItemStorage {
	protected final T wrapped;

	protected WrapperMountedItemStorage(MountedItemStorageType<?> type, T wrapped) {
		super(type);
		this.wrapped = wrapped;
	}

	@Override
	public int size() {
		return this.wrapped.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return this.wrapped.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return this.wrapped.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return this.wrapped.getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return this.wrapped.isValid(index, resource);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return this.wrapped.insert(index, resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return this.wrapped.extract(index, resource, amount, transaction);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		this.wrapped.set(index, resource, amount);
	}

	public static ItemStacksResourceHandler copyToItemStackHandler(ResourceHandler<ItemResource> handler) {
		ItemStacksResourceHandler copy = new ItemStacksResourceHandler(handler.size());
		for (int i = 0; i < handler.size(); i++) {
			ItemStack stack = ItemUtil.getStack(handler, i)
				.copy();
			copy.set(i, ItemResource.of(stack), stack.getCount());
		}
		return copy;
	}
}
