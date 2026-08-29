package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;

public class BasinGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(BasinBlock.FACING));
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		if (state.getValue(BasinBlock.FACING).getAxis().isVertical())
			return AssetLookup.partialBaseVariant(ctx, prov);
		return AssetLookup.partialBaseVariant(ctx, prov, "directional");
	}

}
