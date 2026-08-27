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
		return getCapacityAsInt(0, FluidResource.EMPTY);
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
	}

	@Override
	protected void onContentsChanged(int index, FluidStack previousContents) {
		super.onContentsChanged(index, previousContents);
		updateCallback.accept(getFluid());
	}

}
