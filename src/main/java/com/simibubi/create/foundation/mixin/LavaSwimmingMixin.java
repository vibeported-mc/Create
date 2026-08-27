package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public abstract class LavaSwimmingMixin extends Entity {
	private LavaSwimmingMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	/**
	 * 26.2 split travelling through lava out of {@code travel} into a method of its own, so the slice
	 * that used to narrow the injection to the lava branch is no longer needed - the whole method is
	 * that branch.
	 */
	@Inject(method = "travelInLava(Lnet/minecraft/world/phys/Vec3;DZD)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
		shift = Shift.AFTER, ordinal = 0))
	private void create$onLavaTravel(Vec3 input, double baseGravity, boolean isFalling, double oldY,
		CallbackInfo ci) {
		ItemStack bootsStack = DivingBootsItem.getWornItem(this);
		if (AllItems.NETHERITE_DIVING_BOOTS.isIn(bootsStack))
			setDeltaMovement(getDeltaMovement().multiply(DivingBootsItem.getMovementMultiplier((LivingEntity) (Object) this)));
	}
}
