package com.simibubi.create.foundation.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Stack-shaped conveniences over the 26.2 fluid transfer API, mirroring
 * {@link com.simibubi.create.foundation.item.ItemHandlerHelpers} for items.
 * <p>
 * The old {@code IFluidHandler} expressed "don't actually do it" with a {@code FluidAction}; the new
 * API expresses it by rolling a transaction back instead, which is what the {@code simulate} flags
 * here do.
 */
public class FluidHandlerHelpers {

	public static FluidStack getFluidInTank(ResourceHandler<FluidResource> handler, int tank) {
		return FluidUtil.getStack(handler, tank);
	}

	public static int getTankCapacity(ResourceHandler<FluidResource> handler, int tank) {
		return handler.getCapacityAsInt(tank, FluidResource.EMPTY);
	}

	public static boolean isFluidValid(ResourceHandler<FluidResource> handler, int tank, FluidStack stack) {
		return handler.isValid(tank, FluidResource.of(stack));
	}

	/**
	 * @return how much was accepted
	 */
	public static int fill(ResourceHandler<FluidResource> handler, FluidStack resource, boolean simulate) {
		if (resource.isEmpty())
			return 0;
		try (Transaction transaction = Transaction.openRoot()) {
			int filled = handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
			if (!simulate)
				transaction.commit();
			return filled;
		}
	}

	/**
	 * Drain a specific fluid.
	 */
	public static FluidStack drain(ResourceHandler<FluidResource> handler, FluidStack resource, boolean simulate) {
		if (resource.isEmpty())
			return FluidStack.EMPTY;
		FluidResource wanted = FluidResource.of(resource);
		try (Transaction transaction = Transaction.openRoot()) {
			int drained = handler.extract(wanted, resource.getAmount(), transaction);
			if (drained <= 0)
				return FluidStack.EMPTY;
			if (!simulate)
				transaction.commit();
			return wanted.toStack(drained);
		}
	}

	/**
	 * Drain up to {@code maxDrain} of whatever the handler holds first.
	 */
	public static FluidStack drain(ResourceHandler<FluidResource> handler, int maxDrain, boolean simulate) {
		if (maxDrain <= 0)
			return FluidStack.EMPTY;
		for (int tank = 0; tank < handler.size(); tank++) {
			FluidResource resource = handler.getResource(tank);
			if (resource.isEmpty())
				continue;
			try (Transaction transaction = Transaction.openRoot()) {
				int drained = handler.extract(resource, maxDrain, transaction);
				if (drained <= 0)
					continue;
				if (!simulate)
					transaction.commit();
				return resource.toStack(drained);
			}
		}
		return FluidStack.EMPTY;
	}

}
