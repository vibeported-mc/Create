package com.simibubi.create.content.trains.track;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;

public class TrackBlockStateGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return state.getValue(TrackBlock.SHAPE)
			.getModelRotation();
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		TrackShape value = state.getValue(TrackBlock.SHAPE);
		if (value == TrackShape.NONE)
			return BlockModelGenerators.plainVariant(prov.mcLoc("block/air"));
		return BlockModelGenerators.plainVariant(Create.asResource("block/track/" + value.getModel()));
	}

}
