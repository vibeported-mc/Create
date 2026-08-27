package com.simibubi.create.content.logistics.crate;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A creative, endless source of one item.
 * <p>
 * Nothing here mutates, so there is no state to snapshot and transactions need no participation:
 * extracting always succeeds and rolling back costs nothing.
 */
public class BottomlessItemHandler implements ResourceHandler<ItemResource> {

	private Supplier<ItemStack> suppliedItemStack;

	public BottomlessItemHandler(Supplier<ItemStack> suppliedItemStack) {
		this.suppliedItemStack = suppliedItemStack;
	}

	private ItemStack supplied(int index) {
		if (index == 1)
			return ItemStack.EMPTY;
		ItemStack stack = suppliedItemStack.get();
		return stack == null ? ItemStack.EMPTY : stack;
	}

	@Override
	public int size() {
		return 2;
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(supplied(index));
	}

	@Override
	public long getAmountAsLong(int index) {
		ItemStack stack = supplied(index);
		return stack.isEmpty() ? 0 : stack.getMaxStackSize();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return resource.isEmpty() ? 64 : resource.getMaxStackSize();
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		ItemStack stack = supplied(index);
		if (stack.isEmpty() || !resource.matches(stack))
			return 0;
		return Math.min(stack.getMaxStackSize(), amount);
	}

}
