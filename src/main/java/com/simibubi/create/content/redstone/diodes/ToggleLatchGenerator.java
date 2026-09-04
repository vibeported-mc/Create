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

public class ToggleLatchGenerator extends AbstractDiodeGenerator {

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot TOP = TextureSlot.create("top");
		static final TextureSlot TORCH = TextureSlot.create("torch");
	}


	@Override
	protected <T extends Block> List<MultiVariant> createModels(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov) {
		String name = ctx.getName();
		List<MultiVariant> models = new ArrayList<>(4);
		Identifier off = existing("latch_off");
		Identifier on = existing("latch_on");

		models.add(BlockModelGenerators.plainVariant(off));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(off)
			.texture(Client.TOP, new Material(texture(ctx, "powered")))
			.build(prov.modLoc("block/" + name + "_off_powered"))));
		models.add(BlockModelGenerators.plainVariant(on));
		models.add(BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(on)
			.texture(Client.TOP, new Material(texture(ctx, "powered_powering")))
			.build(prov.modLoc("block/" + name + "_on_powered"))));

		return models;
	}

	@Override
	protected int getModelIndex(BlockState state) {
		return (state.getValue(ToggleLatchBlock.POWERING) ? 2 : 0) + (state.getValue(ToggleLatchBlock.POWERED) ? 1 : 0);
	}

}
