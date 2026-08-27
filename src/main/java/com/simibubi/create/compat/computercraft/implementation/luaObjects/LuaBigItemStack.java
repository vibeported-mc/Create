package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import java.util.Map;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.foundation.utility.GlobalRegistryAccess;

import dan200.computercraft.api.detail.VanillaDetailRegistries;

public class LuaBigItemStack implements LuaComparable {
	private final BigItemStack stack;

	public LuaBigItemStack(BigItemStack stack) {
		this.stack = stack;
	}

	@Override
	public Map<?, ?> getTableRepresentation() {
		Map<String, Object> details = VanillaDetailRegistries.ITEM_STACK.getDetails(GlobalRegistryAccess.getOrThrow(), stack.stack);
		// Add count to the details
		details.put("count", stack.count);
		return details;
	}
}
