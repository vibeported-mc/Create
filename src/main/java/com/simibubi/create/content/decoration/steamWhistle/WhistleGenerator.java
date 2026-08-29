package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public class WhistleGenerator extends SpecialBlockStateGen {

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(WhistleBlock.FACING));
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		String wall = state.getValue(WhistleBlock.WALL) ? "wall" : "floor";
		String size = state.getValue(WhistleBlock.SIZE)
			.getSerializedName();
		boolean powered = state.getValue(WhistleBlock.POWERED);
		MultiVariant model = AssetLookup.partialBaseVariant(ctx, prov, size, wall);
		if (!powered)
			return model;
		Identifier parentLocation = AssetLookup.partialBaseModel(ctx, prov, size, wall);
		return BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(parentLocation)
			.texture(TextureSlot.create("2"), new Material(Create.asResource("block/copper_redstone_plate_powered")))
			.build(prov.modLoc(parentLocation.getPath() + "_powered")));
	}

}
