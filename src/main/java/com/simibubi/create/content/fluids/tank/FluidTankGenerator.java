package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public class FluidTankGenerator extends SpecialBlockStateGen {

	private String prefix;

	public FluidTankGenerator() {
		this("");
	}

	public FluidTankGenerator(String prefix) {
		this.prefix = prefix;
	}

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return 0;
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		Boolean top = state.getValue(FluidTankBlock.TOP);
		Boolean bottom = state.getValue(FluidTankBlock.BOTTOM);
		Shape shape = state.getValue(FluidTankBlock.SHAPE);

		String shapeName = "middle";
		if (top && bottom)
			shapeName = "single";
		else if (top)
			shapeName = "top";
		else if (bottom)
			shapeName = "bottom";

		String modelName = shapeName + (shape == Shape.PLAIN ? "" : "_" + shape.getSerializedName());

		if (!prefix.isEmpty())
			return BlockModelGenerators.plainVariant(prov.getBuilder()
				.parent(prov.modLoc("block/fluid_tank/block_" + modelName))
				.texture(TextureSlot.create("0"), new Material(prov.modLoc("block/" + prefix + "casing")))
				.texture(TextureSlot.create("1"), new Material(prov.modLoc("block/" + prefix + "fluid_tank")))
				.texture(TextureSlot.create("3"), new Material(prov.modLoc("block/" + prefix + "fluid_tank_window")))
				.texture(TextureSlot.create("4"), new Material(prov.modLoc("block/" + prefix + "casing")))
				.texture(TextureSlot.create("5"),
					new Material(prov.modLoc("block/" + prefix + "fluid_tank_window_single")))
				.texture(TextureSlot.PARTICLE, new Material(prov.modLoc("block/" + prefix + "fluid_tank")))
				.build(prov.modLoc("block/" + prefix + modelName)));

		return AssetLookup.partialBaseVariant(ctx, prov, modelName);
	}

}
