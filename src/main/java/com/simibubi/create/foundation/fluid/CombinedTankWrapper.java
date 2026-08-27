package com.simibubi.create.foundation.fluid;

import java.util.Collection;
import net.createmod.catnip.api.data.Iterate;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Presents several fluid handlers as one.
 * <p>
 * NeoForge ships {@code CombinedResourceHandler}, but Create's version also spreads a fill across
 * handlers and can be told to prefer a tank that already holds the same fluid, so it keeps its own
 * implementation.
 */
public class CombinedTankWrapper implements ResourceHandler<FluidResource> {

	protected final ResourceHandler<FluidResource>[] itemHandler;
	protected final int[] baseIndex;
	protected final int tankCount;
	protected boolean enforceVariety;

	@SuppressWarnings("unchecked")
	public CombinedTankWrapper(Collection<? extends ResourceHandler<FluidResource>> fluidHandlers) {
		this(fluidHandlers.toArray(ResourceHandler[]::new));
	}

	@SafeVarargs
	public CombinedTankWrapper(ResourceHandler<FluidResource>... fluidHandlers) {
		this.itemHandler = fluidHandlers;
		this.baseIndex = new int[fluidHandlers.length];
		int index = 0;
		for (int i = 0; i < fluidHandlers.length; i++) {
			index += fluidHandlers[i].size();
			baseIndex[i] = index;
		}
		this.tankCount = index;
	}

	public CombinedTankWrapper enforceVariety() {
		enforceVariety = true;
		return this;
	}

	@Override
	public int size() {
		return tankCount;
	}

	@Override
	public FluidResource getResource(int tank) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).getResource(getSlotFromIndex(tank, index));
	}

	@Override
	public long getAmountAsLong(int tank) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).getAmountAsLong(getSlotFromIndex(tank, index));
	}

	@Override
	public long getCapacityAsLong(int tank, FluidResource resource) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).getCapacityAsLong(getSlotFromIndex(tank, index), resource);
	}

	@Override
	public boolean isValid(int tank, FluidResource resource) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).isValid(getSlotFromIndex(tank, index), resource);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).insert(getSlotFromIndex(tank, index), resource, amount, transaction);
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		int index = getIndexForSlot(tank);
		return getHandlerFromIndex(index).extract(getSlotFromIndex(tank, index), resource, amount, transaction);
	}

	/**
	 * First pass looks only at handlers already holding this fluid, so a fill lands beside its own
	 * kind rather than starting a new tank.
	 */
	@Override
	public int insert(FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;

		int filled = 0;
		boolean fittingHandlerFound = false;

		Outer: for (boolean searchPass : Iterate.trueAndFalse) {
			for (ResourceHandler<FluidResource> handler : itemHandler) {

				for (int i = 0; i < handler.size(); i++)
					if (searchPass && resource.equals(handler.getResource(i)))
						fittingHandlerFound = true;

				if (searchPass && !fittingHandlerFound)
					continue;

				int filledIntoCurrent = handler.insert(resource, amount - filled, transaction);
				filled += filledIntoCurrent;

				if (filled == amount)
					break Outer;
				if (fittingHandlerFound && (enforceVariety || filledIntoCurrent != 0))
					break Outer;
			}
		}

		return filled;
	}

	@Override
	public int extract(FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;

		int drained = 0;
		for (ResourceHandler<FluidResource> handler : itemHandler) {
			drained += handler.extract(resource, amount - drained, transaction);
			if (drained == amount)
				break;
		}
		return drained;
	}

	protected int getIndexForSlot(int slot) {
		if (slot < 0)
			return -1;
		for (int i = 0; i < baseIndex.length; i++)
			if (slot - baseIndex[i] < 0)
				return i;
		return -1;
	}

	protected ResourceHandler<FluidResource> getHandlerFromIndex(int index) {
		if (index < 0 || index >= itemHandler.length)
			return EmptyResourceHandler.instance();
		return itemHandler[index];
	}

	protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndex.length)
			return slot;
		return slot - baseIndex[index - 1];
	}
}
