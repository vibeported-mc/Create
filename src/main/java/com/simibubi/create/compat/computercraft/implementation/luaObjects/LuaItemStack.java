package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import java.util.Map;

import com.simibubi.create.foundation.utility.GlobalRegistryAccess;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import net.minecraft.world.item.ItemStack;

public class LuaItemStack implements LuaComparable {
	private final ItemStack stack;

	public LuaItemStack(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public Map<?, ?> getTableRepresentation() {
		return VanillaDetailRegistries.ITEM_STACK.getDetails(GlobalRegistryAccess.getOrThrow(), stack);
	}
}
