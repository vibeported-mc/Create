package com.simibubi.create.foundation.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * A stack-backed item handler whose slots can also be written directly.
 * <p>
 * 26.2's {@link ItemStacksResourceHandler} has a {@code set} but does not declare
 * {@link net.neoforged.neoforge.transfer.IndexModifier}, so on its own it cannot stand in where Create
 * asks for a {@link ModifiableItemHandler}. This pairs the two.
 */
public class ItemStackHandler extends ItemStacksResourceHandler implements ModifiableItemHandler {

	public ItemStackHandler(int size) {
		super(size);
	}

	public ItemStackHandler(NonNullList<ItemStack> stacks) {
		super(stacks);
	}
}
