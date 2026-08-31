package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;

@Mixin(MinecartBehavior.class)
public interface MinecartBehaviorAccessor {
	@Accessor("minecart")
	AbstractMinecart create$getMinecart();
}
