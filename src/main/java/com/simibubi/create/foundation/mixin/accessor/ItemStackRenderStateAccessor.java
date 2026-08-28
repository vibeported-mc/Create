package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Create draws several of its items itself, replacing a baked item model's geometry. The transform
 * for the display context being drawn lives on the layer that model builds, so that layer has to be
 * reachable to keep the item positioned the way its own model asks for.
 */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
	@Accessor("layers")
	ItemStackRenderState.LayerRenderState[] create$getLayers();

	@Accessor("activeLayerCount")
	int create$getActiveLayerCount();
}
