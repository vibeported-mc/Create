package com.simibubi.create.foundation.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The block entity data an item carries.
 * <p>
 * Minecraft 26.2 replaced the loose {@code CustomData} on this component with
 * {@link TypedEntityData}, which keeps the block entity's type beside the tag rather than as an "id"
 * entry inside it. Create reads and writes that tag in a handful of block items, so the pairing
 * lives here instead of at every call site.
 */
public class BlockEntityDataHelper {

	/**
	 * The stored tag, without the type entry, or an empty tag if the stack carries none.
	 */
	public static CompoundTag getTag(ItemStack stack) {
		TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		return data == null ? new CompoundTag() : data.copyTagWithoutId();
	}

	public static void setTag(ItemStack stack, BlockEntityType<?> type, CompoundTag tag) {
		stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, tag));
	}

	private BlockEntityDataHelper() {
	}

}
