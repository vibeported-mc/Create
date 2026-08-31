package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A handler that stands in front of another one, so a subclass can say no.
 * <p>
 * Only the indexed insert and extract are delegated, deliberately. {@link ResourceHandler}'s
 * whole-handler insert and extract are left at their defaults, which walk the slots and call the
 * indexed ones - so a subclass that refuses a transfer refuses it whichever way it is asked for.
 * NeoForge's {@code DelegatingResourceHandler} forwards those two straight to the handler behind it,
 * which would step over the subclass entirely.
 */
public class ItemHandlerWrapper implements ResourceHandler<ItemResource> {

	private ResourceHandler<ItemResource> wrapped;

	public ItemHandlerWrapper(ResourceHandler<ItemResource> wrapped) {
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

}
