package com.simibubi.create.foundation.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Lets a {@link CustomArmPoseItem} say how the arm holding it is posed.
 * <p>
 * Create used to mix into the player renderer for this; 26.2's renderer asks the item's client
 * extension itself, so the mixin is gone and the items carry this instead.
 */
public class CustomArmPoseClientExtension implements IClientItemExtensions {
	public static final CustomArmPoseClientExtension INSTANCE = new CustomArmPoseClientExtension();

	@Override
	public @Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
		if (entity instanceof AbstractClientPlayer player && stack.getItem() instanceof CustomArmPoseItem item)
			return item.getArmPose(stack, player, hand);
		return null;
	}
}
