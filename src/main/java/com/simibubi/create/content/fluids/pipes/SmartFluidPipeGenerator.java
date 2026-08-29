package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;

public class SmartFluidPipeGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		AttachFace attachFace = state.getValue(SmartFluidPipeBlock.FACE);
		return attachFace == AttachFace.CEILING ? 180 : attachFace == AttachFace.FLOOR ? 0 : 270;
	}

	@Override
	protected int getYRotation(BlockState state) {
		AttachFace attachFace = state.getValue(SmartFluidPipeBlock.FACE);
		int angle = horizontalAngle(state.getValue(SmartFluidPipeBlock.FACING));
		return angle + (attachFace == AttachFace.CEILING ? 180 : 0);
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		return AssetLookup.partialBaseVariant(ctx, prov);
	}

}
