package com.simibubi.create.content.decoration.bracket;

import java.util.Optional;

import com.simibubi.create.foundation.data.DirectionalAxisBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BracketGenerator extends DirectionalAxisBlockStateGen {

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot BRACKET = TextureSlot.create("bracket");
		static final TextureSlot PLATE = TextureSlot.create("plate");
	}


	private String material;

	public BracketGenerator(String material) {
		this.material = material;
	}

	@Override
	public <T extends Block> String getModelPrefix(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		return "";
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		String type = state.getValue(BracketBlock.TYPE)
			.getSerializedName();
		boolean vertical = state.getValue(BracketBlock.FACING)
			.getAxis()
			.isVertical();

		String path = "block/bracket/" + type + "/" + (vertical ? "ground" : "wall");

		return BlockModelGenerators.plainVariant(prov.getBuilder()
			.parent(prov.modLoc(path))
			.texture(Client.BRACKET, new Material(prov.modLoc("block/bracket_" + material)))
			.texture(Client.PLATE, new Material(prov.modLoc("block/bracket_plate_" + material)))
			.build(prov.modLoc(path + "_" + material)));
	}

	/**
	 * The item model for a bracket of this material.
	 *
	 * Returns the generator itself rather than a builder transform, so a call site can hold it
	 * behind {@code .model(() -> ...)} and never load this class on a dedicated server. The class
	 * cannot be loaded there at all: {@link #getModel} returns a {@link MultiVariant}, and the JVM
	 * resolves a method's descriptor when it links the class that declares it.
	 */
	public static NonNullBiConsumer<DataGenContext<Item, BracketBlockItem>, RegistrateItemModelGenerator> itemModel(
		String material) {
		return (c, p) -> {
			ModelTemplate template =
				new ModelTemplate(Optional.of(p.modLoc("block/bracket/item")), Optional.empty(), Client.BRACKET, Client.PLATE);
			p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
				.put(Client.BRACKET, new Material(p.modLoc("block/bracket_" + material)))
				.put(Client.PLATE, new Material(p.modLoc("block/bracket_plate_" + material))));
		};
	}

}
