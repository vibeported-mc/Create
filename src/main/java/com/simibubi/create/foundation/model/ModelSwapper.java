package com.simibubi.create.foundation.model;

import java.util.Map;
import java.util.function.Function;

import com.simibubi.create.foundation.block.render.CustomBlockModels;
import com.simibubi.create.foundation.item.render.CustomItemModels;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;

import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Replaces baked models with Create's own wrappers once baking has finished.
 * <p>
 * Minecraft 26.2 bakes straight to {@code BlockState -> BlockStateModel} and
 * {@code Identifier -> ItemModel} rather than keying everything on a model location, so the swap
 * walks a block's states rather than its model locations.
 */
public class ModelSwapper {

	protected CustomBlockModels customBlockModels = new CustomBlockModels();
	protected CustomItemModels customItemModels = new CustomItemModels();

	public CustomBlockModels getCustomBlockModels() {
		return customBlockModels;
	}

	public CustomItemModels getCustomItemModels() {
		return customItemModels;
	}

	public void onModelBake(ModelEvent.ModifyBakingResult event) {
		ModelBakery.BakingResult result = event.getBakingResult();
		Map<BlockState, BlockStateModel> blockStateModels = result.blockStateModels();
		Map<Identifier, ItemModel> itemModels = result.itemStackModels();

		customBlockModels.forEach((block, modelFunc) -> swapBlockModels(blockStateModels, block, modelFunc));
		customItemModels.forEach((item, modelFunc) -> swapItemModel(itemModels, item, modelFunc));
		CustomRenderedItems.forEach(
			(item, renderer) -> swapItemModel(itemModels, item, model -> new CustomRenderedItemModel(model, renderer)));
	}

	public void registerListeners(IEventBus modEventBus) {
		modEventBus.addListener(this::onModelBake);
	}

	public static void swapBlockModels(Map<BlockState, BlockStateModel> models, Block block,
		Function<BlockStateModel, ? extends BlockStateModel> factory) {
		block.getStateDefinition()
			.getPossibleStates()
			.forEach(state -> models.computeIfPresent(state, (ignored, model) -> factory.apply(model)));
	}

	public static void swapItemModel(Map<Identifier, ItemModel> models, Item item,
		Function<ItemModel, ? extends ItemModel> factory) {
		models.computeIfPresent(RegisteredObjectsHelper.getKeyOrThrow(item),
			(ignored, model) -> factory.apply(model));
	}

}
