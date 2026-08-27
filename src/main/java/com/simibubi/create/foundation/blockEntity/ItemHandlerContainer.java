package com.simibubi.create.foundation.blockEntity;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import com.simibubi.create.foundation.item.ModifiableItemHandler;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemHandlerContainer implements Container {
	protected final ModifiableItemHandler inv;

	public ItemHandlerContainer(ModifiableItemHandler inv) {
		this.inv = inv;
	}

	/**
	 * Returns the size of this inventory.
	 */
	@Override
	public int getContainerSize() {
		return inv.size();
	}

	/**
	 * Returns the stack in this slot.
	 * <p>
	 * Container's contract asks for a modifiable reference, but a resource handler stores a resource
	 * and an amount rather than a stack, so this is necessarily a copy. Callers that mutate the result
	 * have to write it back with {@link #setItem}.
	 */
	@Override
	public ItemStack getItem(int slot) {
		return ItemHandlerHelpers.getStackInSlot(inv, slot);
	}

	/**
	 * Attempts to remove n items from the specified slot.  Returns the split stack that was removed.  Modifies the inventory.
	 */
	@Override
	public ItemStack removeItem(int slot, int count) {
		return ItemHandlerHelpers.extractItem(inv, slot, count, false);
	}

	/**
	 * Sets the contents of this slot to the provided stack.
	 */
	@Override
	public void setItem(int slot, ItemStack stack) {
		ItemHandlerHelpers.setStackInSlot(ItemHandlerHelpers, inv, slot, stack);
	}

	/**
	 * Removes the stack contained in this slot from the underlying handler, and returns it.
	 */
	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack s = getItem(index);
		if (s.isEmpty())
			return ItemStack.EMPTY;

		setItem(index, ItemStack.EMPTY);
		return s;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < inv.size(); i++) {
			if (!inv.getResource(i)
				.isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return ItemHandlerHelpers.isItemValid(inv, slot, stack);
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.size(); i++)
			inv.set(i, ItemResource.EMPTY, 0);
	}

	//The following methods are never used by vanilla in crafting.  They are defunct as mods need not override them.
	@Override
	public int getMaxStackSize() {
		return 0;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}
}
