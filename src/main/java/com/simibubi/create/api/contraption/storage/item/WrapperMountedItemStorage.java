package com.simibubi.create.api.contraption.storage.item;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;

/**
 * Partial implementation of a MountedItemStorage that wraps an item handler.
 */
public abstract class WrapperMountedItemStorage<T extends ModifiableItemHandler> extends MountedItemStorage {
	protected final T wrapped;

	protected WrapperMountedItemStorage(MountedItemStorageType<?> type, T wrapped) {
		super(type);
		this.wrapped = wrapped;
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		ItemHandlerHelpers.setStackInSlot(this.wrapped, slot, stack);
	}

	@Override
	public int getSlots() {
		return this.wrapped.size();
	}

	@Override
	@NotNull
	public ItemStack getStackInSlot(int slot) {
		return this.wrapped.getStackInSlot(slot);
	}

	@Override
	@NotNull
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		return this.wrapped.insertItem(slot, stack, simulate);
	}

	@Override
	@NotNull
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return this.wrapped.extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return this.wrapped.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return this.wrapped.isItemValid(slot, stack);
	}

	public static ItemStacksResourceHandler copyToItemStackHandler(ResourceHandler<ItemResource> handler) {
		ItemStacksResourceHandler copy = new ItemStacksResourceHandler(handler.size());
		for (int i = 0; i < handler.size(); i++) {
			ItemHandlerHelpers.setStackInSlot(copy, i, ItemHandlerHelpers.getStackInSlot(handler, i).copy());
		}
		return copy;
	}
}
