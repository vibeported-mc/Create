package com.simibubi.create.api.contraption.storage.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import net.neoforged.neoforge.fluids.FluidStack;
/**
 * Partial implementation of a MountedFluidStorage that wraps a fluid handler.
 */
public abstract class WrapperMountedFluidStorage<T extends ResourceHandler<FluidResource>> extends MountedFluidStorage {
	protected final T wrapped;

	protected WrapperMountedFluidStorage(MountedFluidStorageType<?> type, T wrapped) {
		super(type);
		this.wrapped = wrapped;
	}

	@Override
	public int size() {
		return this.wrapped.size();
	}

	@Override
	public FluidResource getResource(int tank) {
		return this.wrapped.getResource(tank);
	}

	@Override
	public long getAmountAsLong(int tank) {
		return this.wrapped.getAmountAsLong(tank);
	}

	@Override
	public long getCapacityAsLong(int tank, FluidResource resource) {
		return this.wrapped.getCapacityAsLong(tank, resource);
	}

	@Override
	public boolean isValid(int tank, FluidResource resource) {
		return this.wrapped.isValid(tank, resource);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		return this.wrapped.insert(tank, resource, amount, transaction);
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		return this.wrapped.extract(tank, resource, amount, transaction);
	}
}
