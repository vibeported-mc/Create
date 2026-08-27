package com.simibubi.create.foundation.item.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Hands an item over to one of Create's own renderers.
 * <p>
 * 26.2 dropped the block-entity-without-level renderer that used to draw these items; a
 * {@link net.minecraft.client.renderer.special.SpecialModelRenderer} takes its place, reached
 * through a layer of the item's render state. Create swaps this wrapper in over the item's baked
 * model rather than declaring it in the item's json, so the wrapped model stays available for the
 * renderer to draw as the item's base.
 */
public class CustomRenderedItemModel implements ItemModel {

	private final ItemModel originalModel;
	private final CustomRenderedItemModelRenderer renderer;

	public CustomRenderedItemModel(ItemModel originalModel, CustomRenderedItemModelRenderer renderer) {
		this.originalModel = originalModel;
		this.renderer = renderer;
	}

	@Override
	public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
		ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		output.appendModelIdentityElement(this);
		ItemStackRenderState.LayerRenderState layer = output.newLayer();
		if (item.hasFoil()) {
			layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
			output.setAnimated();
		}
		layer.setupSpecialModel(renderer,
			new CustomItemRenderContext(item, displayContext, originalModel, level, owner, seed));
	}

	public ItemModel getOriginalModel() {
		return originalModel;
	}

}
