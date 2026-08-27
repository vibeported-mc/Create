package com.simibubi.create.foundation.item.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Everything one of Create's item renderers needs, captured while the item's render state is built.
 * <p>
 * {@link net.minecraft.client.renderer.special.SpecialModelRenderer#submit} is handed only an
 * argument of the renderer's own choosing, so the display context and the item's base model - which
 * the old renderer received directly - travel through here instead.
 */
public record CustomItemRenderContext(ItemStack stack, ItemDisplayContext displayContext,
	@Nullable ItemModel baseModel, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {

	public CustomItemRenderContext(ItemStack stack, ItemDisplayContext displayContext) {
		this(stack, displayContext, null, null, null, 0);
	}

}
