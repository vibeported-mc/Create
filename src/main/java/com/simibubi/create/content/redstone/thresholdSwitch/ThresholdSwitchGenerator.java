package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.createmod.catnip.api.lang.Lang;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public class ThresholdSwitchGenerator extends SpecialBlockStateGen {

	private static final TextureSlot LEVEL = TextureSlot.create("level");

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(ThresholdSwitchBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		int level = state.getValue(ThresholdSwitchBlock.LEVEL);
		String path = "block/threshold_switch/block_" + Lang.asId(state.getValue(ThresholdSwitchBlock.TARGET)
			.name());
		return BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(Create.asResource(path))
			.texture(LEVEL, new Material(Create.asResource("block/threshold_switch/level_" + level)))
			.build(prov.modLoc(path + "_" + level)));
	}

}
