package com.simibubi.create.foundation.fluid;

import java.util.function.Consumer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A single tank that reports its contents whenever they change.
 * <p>
 * The callback now fires from the transactional hook, so it is not run for a transfer that ends up
 * being rolled back.
 */
public class SmartFluidTank extends FluidStacksResourceHandler {

	private Consumer<FluidStack> updateCallback;

	public SmartFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
		super(1, capacity);
		this.updateCallback = updateCallback;
	}

	public FluidStack getFluid() {
		return FluidUtil.getStack(this, 0);
	}

	public void setFluid(FluidStack stack) {
		set(0, FluidResource.of(stack), stack.getAmount());
		updateCallback.accept(stack);
	}

	public int getCapacity() {
		return capacity;
	}

	/**
	 * Puts fluid in with no regard for capacity, and still inside the transaction.
	 * <p>
	 * A hose pulley empties a whole block at a time and has to put what is left of it somewhere, which
	 * used to be done with a bare {@link #setFluid}: that writes past whatever transaction it is inside.
	 * Widening the tank for the length of the call borrows the handler's own journal instead, so a pull
	 * that ends up rolled back leaves nothing behind. Only for a caller that has nowhere else to put the
	 * fluid - anything else should insert and respect what it is told it may.
	 */
	public int insertBeyondCapacity(FluidResource resource, int amount, TransactionContext transaction) {
		int stated = capacity;
		capacity = stated + amount;

		try {
			return insert(0, resource, amount, transaction);
		} finally {
			capacity = stated;
		}
	}

	/**
	 * Multiblock tanks grow and shrink, so the capacity stays settable the way it was.
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getSpace() {
		return getCapacity() - getFluidAmount();
	}

	public boolean isEmpty() {
		return getResource(0).isEmpty();
	}

	public int getFluidAmount() {
		return getAmountAsInt(0);
	}

	/**
	 * The handler serializes through ValueIO now; these bridge the CompoundTag form Create's block
	 * entities still read and write.
	 */
	public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag tag) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		serialize(output);
		tag.merge(output.buildResult());
		return tag;
	}

	public void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
		deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));

		// GAMETEST FIX - a stopgap: existing worlds want a datafixer, not a read-time fallback.
		// A tank written before the handler serialized through ValueIO put everything it held under a
		// single "Fluid" tag. Those are still about - in worlds saved by an older version, in schematics,
		// in the structures the game tests are built from - and read the new way they come back empty,
		// which quietly pours away whatever was in them.
		if (isEmpty() && tag.contains("Fluid"))
			setFluid(FluidHelper.parseOptional(registries, tag.getCompoundOrEmpty("Fluid")));
	}

	@Override
	protected void onContentsChanged(int index, FluidStack previousContents) {
		super.onContentsChanged(index, previousContents);
		updateCallback.accept(getFluid());
	}

}
