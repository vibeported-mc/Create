package com.simibubi.create.content.redstone.diodes;

import java.util.ArrayList;
import java.util.List;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredLatchGenerator extends AbstractDiodeGenerator {

	private static final TextureSlot TOP = TextureSlot.create("top");
	private static final TextureSlot TORCH = TextureSlot.create("torch");

	@Override
	protected <T extends Block> List<MultiVariant> createModels(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov) {
		List<MultiVariant> models = new ArrayList<>(2);
		String name = ctx.getName();
		Identifier off = existing("latch_off");
		Identifier on = existing("latch_on");

		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(off)
			.texture(TOP, new Material(texture(ctx, "idle")))
			.build(prov.modLoc("block/" + name))));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(on)
			.texture(TOP, new Material(texture(ctx, "powering")))
			.build(prov.modLoc("block/" + name + "_powered"))));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return state.getValue(PoweredLatchBlock.POWERING) ? 1 : 0;
	}

}
