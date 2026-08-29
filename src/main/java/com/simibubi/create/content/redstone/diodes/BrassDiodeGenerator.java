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

public class BrassDiodeGenerator extends AbstractDiodeGenerator {

	private static final TextureSlot TOP = TextureSlot.create("top");
	private static final TextureSlot TORCH = TextureSlot.create("torch");

	@Override
	protected <T extends Block> List<MultiVariant> createModels(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov) {
		List<MultiVariant> models = new ArrayList<>(4);
		String name = ctx.getName();
		Identifier template = existing(name);

		models.add(BlockModelGenerators.plainVariant(template));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(template)
			.texture(TOP, new Material(texture(ctx, "powered")))
			.build(prov.modLoc("block/" + name + "_powered"))));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(template)
			.texture(TORCH, new Material(poweredTorch()))
			.texture(TOP, new Material(texture(ctx, "powering")))
			.build(prov.modLoc("block/" + name + "_powering"))));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(template)
			.texture(TORCH, new Material(poweredTorch()))
			.texture(TOP, new Material(texture(ctx, "powered_powering")))
			.build(prov.modLoc("block/" + name + "_powered_powering"))));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return (state.getValue(BrassDiodeBlock.POWERING) ^ state.getValue(BrassDiodeBlock.INVERTED) ? 2 : 0)
			+ (state.getValue(BrassDiodeBlock.POWERED) ? 1 : 0);
	}

}
