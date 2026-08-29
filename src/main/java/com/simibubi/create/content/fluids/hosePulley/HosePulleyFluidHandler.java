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

	/**
	 * The pool the hose is hanging in, when nothing is held internally. Both halves of what a tank
	 * reports have to agree: {@link #getResource} already answers with the fluid down there, and
	 * saying nought of it is available leaves every reader holding an empty stack and concluding the
	 * pulley has nothing to give.
	 */
	@Override
	public long getAmountAsLong(int tank) {
		if (internalTank.isEmpty())
			return drainer.getDrainableFluid(rootPosGetter.get()).getAmount();
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

		int available = 1000 + internalTank.getFluidAmount();
		int drained = Math.min(amount, available);
		int leftover = available - drained;

		internalTank.set(0, leftover > 0 ? resource : FluidResource.EMPTY, leftover);
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
