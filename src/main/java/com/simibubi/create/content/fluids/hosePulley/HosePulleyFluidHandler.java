package com.simibubi.create.content.fluids.hosePulley;

import java.util.function.Supplier;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A hose pulley moves fluid between its internal tank and the world.
 * <p>
 * Placing or removing a fluid block cannot be rolled back, so the world half is only simulated while
 * the transaction is open and actually performed from {@link SnapshotJournal#onRootCommit}. The
 * internal tank is a resource handler and takes care of itself.
 */
public class HosePulleyFluidHandler implements ResourceHandler<FluidResource> {

	private SmartFluidTank internalTank;
	private FluidFillingBehaviour filler;
	private FluidDrainingBehaviour drainer;
	private Supplier<BlockPos> rootPosGetter;
	private Supplier<Boolean> predicate;

	private final WorldJournal journal = new WorldJournal();
	private int pendingDeposits;
	private int pendingPulls;
	/** What the pending deposits are made of; the tank may be empty again by the time they run. */
	private FluidResource pendingDepositResource = FluidResource.EMPTY;

	public HosePulleyFluidHandler(SmartFluidTank internalTank, FluidFillingBehaviour filler,
		FluidDrainingBehaviour drainer, Supplier<BlockPos> rootPosGetter, Supplier<Boolean> predicate) {
		this.internalTank = internalTank;
		this.filler = filler;
		this.drainer = drainer;
		this.rootPosGetter = rootPosGetter;
		this.predicate = predicate;
	}

	@Override
	public int size() {
		return internalTank.size();
	}

	@Override
	public FluidResource getResource(int tank) {
		if (internalTank.isEmpty())
			return FluidResource.of(drainer.getDrainableFluid(rootPosGetter.get()));
		return internalTank.getResource(tank);
	}

	@Override
	public long getAmountAsLong(int tank) {
		return internalTank.getAmountAsLong(tank);
	}

	@Override
	public long getCapacityAsLong(int tank, FluidResource resource) {
		return internalTank.getCapacityAsLong(tank, resource);
	}

	@Override
	public boolean isValid(int tank, FluidResource resource) {
		return internalTank.isValid(tank, resource);
	}

	@Override
	public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;
		if (!internalTank.isEmpty() && !resource.equals(internalTank.getResource(0)))
			return 0;
		if (!FluidHelper.hasBlockState(resource.getFluid()))
			return 0;

		int diff = amount;
		int totalAmountAfterFill = diff + internalTank.getFluidAmount();
		boolean deposited = false;

		// A full bucket's worth can be pushed out into the world; only simulate it here.
		if (predicate.get() && totalAmountAfterFill >= 1000
			&& filler.tryDeposit(resource.getFluid(), rootPosGetter.get(), true)) {
			journal.updateSnapshots(transaction);
			pendingDeposits++;
			pendingDepositResource = resource;
			diff -= 1000;
			deposited = true;
		}

		if (diff <= 0) {
			// The deposited bucket was made up partly from what the tank already held.
			if (!resource.isEmpty())
				internalTank.extract(0, resource, -diff, transaction);
			return amount;
		}

		return internalTank.insert(0, resource, diff, transaction) + (deposited ? 1000 : 0);
	}

	@Override
	public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
		if (resource.isEmpty() || amount <= 0)
			return 0;
		if (!internalTank.isEmpty() && !resource.equals(internalTank.getResource(0)))
			return 0;
		if (internalTank.getFluidAmount() >= 1000)
			return internalTank.extract(0, resource, amount, transaction);

		BlockPos pos = rootPosGetter.get();
		FluidStack returned = drainer.getDrainableFluid(pos);
		if (!predicate.get() || returned.isEmpty() || !drainer.pullNext(pos, true))
			return internalTank.extract(0, resource, amount, transaction);

		if (!internalTank.isEmpty() && !resource.equals(internalTank.getResource(0)))
			return internalTank.extract(0, resource, amount, transaction);
		if (!resource.equals(FluidResource.of(returned)))
			return 0;

		journal.updateSnapshots(transaction);
		pendingPulls++;

		int held = internalTank.getFluidAmount();
		int available = 1000 + held;
		int drained = Math.min(amount, available);
		int leftover = available - drained;

		// GAMETEST FIX - the tank is pushed past the capacity it reports, which is what 1.21.1 did too.
		// The honest shape is a buffer that admits it holds two buckets, but that is a number players
		// read off the block, so it waits for a pass where the display can change with it.
		//
		// A block leaves the pool whole, so what is left of it after this caller is served has nowhere to
		// go but the tank, over the top of what the tank says it holds. Anything that would not fit was
		// being quietly dropped, a third of a bucket at a time, all the way across a transfer. It goes in
		// through the transaction rather than by setting the tank outright: set writes past the
		// transaction it is inside, so a pass that only meant to ask what was available left the fluid
		// behind while the pull that would have taken it out of the pool was rolled back - fluid out of
		// nowhere, and a pool that never went down.
		if (leftover > held)
			internalTank.insertBeyondCapacity(resource, leftover - held, transaction);
		else if (leftover < held)
			if (!resource.isEmpty())
				internalTank.extract(0, resource, held - leftover, transaction);

		return drained;
	}

	public SmartFluidTank getInternalTank() {
		return internalTank;
	}

	private record PendingWorldActions(int deposits, int pulls, FluidResource depositResource) {
	}

	private class WorldJournal extends SnapshotJournal<PendingWorldActions> {
		@Override
		protected PendingWorldActions createSnapshot() {
			return new PendingWorldActions(pendingDeposits, pendingPulls, pendingDepositResource);
		}

		@Override
		protected void revertToSnapshot(PendingWorldActions snapshot) {
			pendingDeposits = snapshot.deposits();
			pendingPulls = snapshot.pulls();
			pendingDepositResource = snapshot.depositResource();
		}

		@Override
		protected void onRootCommit(PendingWorldActions originalState) {
			BlockPos pos = rootPosGetter.get();
			for (int i = 0; i < pendingDeposits; i++) {
				if (filler.tryDeposit(pendingDepositResource.getFluid(), pos, false))
					drainer.counterpartActed();
			}
			for (int i = 0; i < pendingPulls; i++) {
				if (drainer.pullNext(pos, false))
					filler.counterpartActed();
			}
			pendingDeposits = 0;
			pendingPulls = 0;
			pendingDepositResource = FluidResource.EMPTY;
		}
	}

}
