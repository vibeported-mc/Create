package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.content.redstone.nixieTube.DoubleFaceAttachedBlock.DoubleAttachFace;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;

public class NixieTubeGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return state.getValue(NixieTubeBlock.FACE)
			.xRot();
	}

	@Override
	protected int getYRotation(BlockState state) {
		DoubleAttachFace face = state.getValue(NixieTubeBlock.FACE);
		return horizontalAngle(state.getValue(NixieTubeBlock.FACING))
			+ (face == DoubleAttachFace.WALL || face == DoubleAttachFace.WALL_REVERSED ? 180 : 0);
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		return BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(prov.modLoc("block/nixie_tube/block"))
			.build(prov.modLoc("block/" + ctx.getName())));
	}

}
