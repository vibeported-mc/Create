package com.simibubi.create.foundation.item;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class SmartInventory extends ItemHandlerContainer implements ModifiableItemHandler {

	protected boolean extractionAllowed;
	protected boolean insertionAllowed;
	protected boolean stackNonStackables;
	protected SyncedStackHandler wrapped;
	protected int stackSize;

	public SmartInventory(int slots, SyncedBlockEntity be) {
		this(slots, be, 64, false);
	}

	public SmartInventory(int slots, SyncedBlockEntity be, BiPredicate<Integer, ItemStack> isValid) {
		this(slots, be, 64, false, isValid);
	}

	public SmartInventory(int slots, SyncedBlockEntity be, int stackSize, boolean stackNonStackables) {
		this(new SyncedStackHandler(slots, be, stackNonStackables, stackSize), stackSize, stackNonStackables);
	}

	public SmartInventory(int slots, SyncedBlockEntity be, int stackSize, boolean stackNonStackables,
		BiPredicate<Integer, ItemStack> isValid) {
		this(new SyncedStackHandler(slots, be, stackNonStackables, stackSize, isValid), stackSize, stackNonStackables);
	}

	public SmartInventory(ModifiableItemHandler inv, int stackSize, boolean stackNonStackables) {
		super(inv);
		this.stackNonStackables = stackNonStackables;
		insertionAllowed = true;
		extractionAllowed = true;
		this.stackSize = stackSize;
		wrapped = (SyncedStackHandler) inv;
	}

	public SmartInventory withMaxStackSize(int maxStackSize) {
		stackSize = maxStackSize;
		wrapped.stackSize = maxStackSize;
		return this;
	}

	public SmartInventory whenContentsChanged(Consumer<Integer> updateCallback) {
		wrapped.whenContentsChange(updateCallback);
		return this;
	}

	public SmartInventory allowInsertion() {
		insertionAllowed = true;
		return this;
	}

	public SmartInventory allowExtraction() {
		extractionAllowed = true;
		return this;
	}

	public SmartInventory forbidInsertion() {
		insertionAllowed = false;
		return this;
	}

	public SmartInventory forbidExtraction() {
		extractionAllowed = false;
		return this;
	}

	@Override
	public int size() {
		return inv.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return inv.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return inv.getAmountAsLong(index);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (!insertionAllowed)
			return 0;
		return inv.insert(index, resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (!extractionAllowed)
			return 0;
		if (stackNonStackables) {
			// Slots holding more than a stack of an unstackable item still hand out at most one
			// stack at a time, as they did before.
			int maxStackSize = resource.getMaxStackSize();
			if (maxStackSize < inv.getAmountAsInt(index))
				amount = Math.min(amount, maxStackSize);
		}
		return inv.extract(index, resource, amount, transaction);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return Math.min(inv.getCapacityAsLong(index, resource), stackSize);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return inv.isValid(index, resource);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		inv.set(index, resource, amount);
	}

	public int getStackLimit(int slot, ItemStack stack) {
		return Math.min(getCapacityAsInt(slot, ItemResource.of(stack)), stack.getMaxStackSize());
	}

	/**
	 * The handler serializes through ValueIO now; these bridge the CompoundTag form that Create's
	 * block entities still read and write.
	 */
	public CompoundTag serializeNBT(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		wrapped.serialize(output);
		return output.buildResult();
	}

	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		wrapped.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt));
	}

	protected static class SyncedStackHandler extends ItemStacksResourceHandler implements ModifiableItemHandler {

		private SyncedBlockEntity blockEntity;
		private boolean stackNonStackables;
		private int stackSize;
		private BiPredicate<Integer, ItemStack> isValid;
		private Consumer<Integer> updateCallback;

		public SyncedStackHandler(int slots, SyncedBlockEntity be, boolean stackNonStackables, int stackSize,
			BiPredicate<Integer, ItemStack> isValid) {
			this(slots, be, stackNonStackables, stackSize);
			this.isValid = isValid;
		}

		public SyncedStackHandler(int slots, SyncedBlockEntity be, boolean stackNonStackables, int stackSize) {
			super(slots);
			this.blockEntity = be;
			this.stackNonStackables = stackNonStackables;
			this.stackSize = stackSize;
		}

		/**
		 * Fires once the outermost transaction commits, so a rolled back transfer no longer marks the
		 * block entity dirty.
		 */
		@Override
		protected void onContentsChanged(int index, ItemStack previousContents) {
			super.onContentsChanged(index, previousContents);
			if (updateCallback != null)
				updateCallback.accept(index);
			blockEntity.notifyUpdate();
		}

		@Override
		protected int getCapacity(int index, ItemResource resource) {
			int base = stackNonStackables ? Item.ABSOLUTE_MAX_STACK_SIZE : super.getCapacity(index, resource);
			return Math.min(base, stackSize);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (isValid == null)
				return super.isValid(index, resource);
			return isValid.test(index, resource.toStack(1));
		}

		public void whenContentsChange(Consumer<Integer> updateCallback) {
			this.updateCallback = updateCallback;
		}

	}

}
