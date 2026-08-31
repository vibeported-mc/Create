package com.simibubi.create.foundation.blockEntity;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemHandlerContainer implements Container {

	protected final ResourceHandler<ItemResource> inv;

	/**
	 * The same inventory, seen as something whose slots can be written.
	 * <p>
	 * 26.2 keeps transfer and direct writes on separate interfaces and provides no type that is both,
	 * so the two halves are held side by side - the shape NeoForge's own {@code ResourceHandlerSlot}
	 * uses for the same reason.
	 */
	protected final IndexModifier<ItemResource> writable;

	public <H extends ResourceHandler<ItemResource> & IndexModifier<ItemResource>> ItemHandlerContainer(H inv) {
		this(inv, inv);
	}

	public ItemHandlerContainer(ResourceHandler<ItemResource> inv, IndexModifier<ItemResource> writable) {
		this.inv = inv;
		this.writable = writable;
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
		return ItemUtil.getStack(inv, slot);
	}

	/**
	 * Attempts to remove n items from the specified slot.  Returns the split stack that was removed.  Modifies the inventory.
	 */
	@Override
	public ItemStack removeItem(int slot, int count) {
		try (Transaction transaction = Transaction.openRoot()) {
			ItemResource transferredResource = inv.getResource(slot);
			int transferred = transferredResource.isEmpty() ? 0 : inv.extract(slot, transferredResource, count, transaction);
			transaction.commit();
			return transferred <= 0 ? ItemStack.EMPTY : transferredResource.toStack(transferred);
		}
	}

	/**
	 * Sets the contents of this slot to the provided stack.
	 */
	@Override
	public void setItem(int slot, ItemStack stack) {
		writable.set(slot, ItemResource.of(stack), stack.getCount());
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
		return inv.isValid(slot, ItemResource.of(stack));
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.size(); i++)
			writable.set(i, ItemResource.EMPTY, 0);
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
