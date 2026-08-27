package com.simibubi.create.foundation.fluid;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * One item, held somewhere a fluid handler is allowed to swap it out.
 * <p>
 * Filling and emptying used to hand back a container item of their own. In 26.2 a handler writes
 * through an {@link ItemAccess} instead, and the access that simply wraps a stack refuses to change
 * which item it holds - which is exactly what a bucket does when it is filled. A one-slot handler
 * gives the fluid handler somewhere it may swap the item, and the result is read back afterwards.
 */
public class ItemFluidAccess {

	private final ItemStacksResourceHandler slot;
	private final ItemAccess access;

	public ItemFluidAccess(ItemStack stack) {
		slot = new ItemStacksResourceHandler(1);
		ItemHandlerHelpers.setStackInSlot(slot, 0, stack);
		access = ItemAccess.forHandlerIndex(slot, 0);
	}

	/**
	 * The fluid handler of the item currently held, or null if it has none.
	 */
	public @Nullable ResourceHandler<FluidResource> handler() {
		return Capabilities.Fluid.ITEM.getCapability(result(), access);
	}

	/**
	 * The item as it stands, after whatever the handler has done to it.
	 */
	public ItemStack result() {
		return ItemHandlerHelpers.getStackInSlot(slot, 0);
	}

}
