package com.simibubi.create.foundation.utility;

import com.mojang.serialization.DynamicOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

/**
 * The ops a codec needs when what it reads or writes points into a registry.
 * <p>
 * Create used to move item and fluid stacks through {@code ItemStack#parseOptional} and friends, which
 * took a {@link HolderLookup.Provider} and wrapped it around the NBT ops for you. 26.2 replaced those
 * with {@code CompoundTag#read}/{@code #store}, whose short forms use bare {@link NbtOps} - enough for
 * plain values, but not for a component that names an enchantment, a potion or anything else a
 * registry owns. Passing these ops instead keeps those resolvable.
 */
public class RegistryNbt {

	public static DynamicOps<Tag> ops(HolderLookup.Provider registries) {
		return registries.createSerializationContext(NbtOps.INSTANCE);
	}

	private RegistryNbt() {
	}

}
