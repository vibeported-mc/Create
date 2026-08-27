package com.simibubi.create.foundation.model;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelData.Builder;

/**
 * A block model that wraps another and gathers extra data about the block it is rendering.
 * <p>
 * Minecraft 26.2 hands the level, position and state straight to
 * {@link BlockStateModel#collectParts}, so there is no separate pass that builds a model's data any
 * more. What the model needs is gathered where it is used instead, through {@link #getModelData};
 * wrapped models still contribute, so a stack of wrappers sees all of it.
 */
public abstract class BakedModelWrapperWithData extends DelegateBlockStateModel {

	public BakedModelWrapperWithData(BlockStateModel originalModel) {
		super(originalModel);
	}

	protected final ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		ModelData blockEntityData = world.getModelData(pos);
		Builder builder = ModelData.builder();
		if (delegate instanceof BakedModelWrapperWithData wrapped)
			wrapped.gatherModelData(builder, world, pos, state, blockEntityData);
		gatherModelData(builder, world, pos, state, blockEntityData);
		return builder.build();
	}

	protected abstract ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world,
		BlockPos pos, BlockState state, ModelData blockEntityData);

}
