package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * {@link ItemEntity#makeFakeItem()} parks the pickup delay at its sentinel value, and nothing
 * public reads it back. Create needs to recognise those fake drops so it does not turn one into a
 * package entity.
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
	@Accessor("pickupDelay")
	int create$getPickupDelay();
}
