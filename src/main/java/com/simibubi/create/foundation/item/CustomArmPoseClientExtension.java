package com.simibubi.create.foundation.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Poses the arm holding a gadget as if it held a crossbow, so the barrel points where the player aims.
 * <p>
 * Create used to mix into the player renderer for this; 26.2's renderer asks the item's client
 * extension itself, so the mixin is gone and the items name this in their registration.
 * <p>
 * It used to ask the item, through a {@code CustomArmPoseItem} interface the items implemented. That
 * put {@link ArmPose} and {@code AbstractClientPlayer} in a method descriptor on
 * {@code PotatoCannonItem} and {@code ZapperItem}, and the JVM resolves an interface's descriptors
 * when it builds an implementor's itable -- so neither item could be loaded on a dedicated server.
 * Both implementations were this same two-line pose, so it lives here instead. An item that one day
 * wants a different pose gets its own extension, which is a client class already.
 */
public class CustomArmPoseClientExtension implements IClientItemExtensions {
	public static final CustomArmPoseClientExtension INSTANCE = new CustomArmPoseClientExtension();

	@Override
	public @Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
		return entity.swinging ? null : ArmPose.CROSSBOW_HOLD;
	}
}
