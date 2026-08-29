package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;

public class PackagerLinkGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return state.getValue(PackagerLinkBlock.FACE) == AttachFace.CEILING ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		Direction facing = state.getValue(PackagerLinkBlock.FACING);
		return horizontalAngle(facing);
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
												BlockState state) {
		String variant =
			state.getValue(PackagerLinkBlock.FACE) == AttachFace.WALL ? "block_horizontal" : "block_vertical";
		if (state.getValue(PackagerLinkBlock.POWERED))
			variant += "_powered";
		return BlockModelGenerators.plainVariant(prov.modLoc("block/stock_link/" + variant));
	}

}
