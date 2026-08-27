package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ItemHandlerWrapper implements ModifiableItemHandler {

	private ModifiableItemHandler wrapped;

	public ItemHandlerWrapper(ModifiableItemHandler wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return wrapped.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return wrapped.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return wrapped.getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return wrapped.isValid(index, resource);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.insert(index, resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.extract(index, resource, amount, transaction);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		wrapped.set(index, resource, amount);
	}

}
