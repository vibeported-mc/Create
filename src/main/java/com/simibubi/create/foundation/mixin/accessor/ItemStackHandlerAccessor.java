package com.simibubi.create.foundation.mixin.accessor;

import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
@Mixin(ItemStacksResourceHandler.class)
public interface ItemStackHandlerAccessor {
	@Accessor("stacks")
	NonNullList<ItemStack> create$getStacks();
}
