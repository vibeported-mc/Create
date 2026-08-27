package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
	@Accessor("renderers")
	Map<EntityType<?>, EntityRenderer<?, ?>> create$getRenderers();

	/**
	 * 26.2 keeps the player's renderers in a map of their own, keyed by model type, where the skin map
	 * used to be.
	 */
	@Accessor("playerRenderers")
	Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> create$getPlayerRenderers();
}
