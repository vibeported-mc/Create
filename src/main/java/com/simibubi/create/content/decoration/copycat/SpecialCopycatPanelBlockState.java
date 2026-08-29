package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpecialCopycatPanelBlockState extends SpecialBlockStateGen {

	private String name;

	public SpecialCopycatPanelBlockState(String name) {
		this.name = name;
	}

	@Override
	protected int getXRotation(BlockState state) {
		return facing(state) == Direction.UP ? 0 : facing(state) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(facing(state));
	}

	private Direction facing(BlockState state) {
		return state.getValue(DirectionalBlock.FACING);
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		return BlockModelGenerators.plainVariant(facing(state).getAxis() == Axis.Y
			? prov.modLoc("block/copycat_panel/" + name + "_vertical")
			: prov.modLoc("block/copycat_panel/" + name));
	}

}
