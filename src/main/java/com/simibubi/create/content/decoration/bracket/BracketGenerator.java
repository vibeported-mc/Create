package com.simibubi.create.content.decoration.bracket;

import java.util.Optional;

import com.simibubi.create.foundation.data.DirectionalAxisBlockStateGen;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BracketGenerator extends DirectionalAxisBlockStateGen {

	private static final TextureSlot BRACKET = TextureSlot.create("bracket");
	private static final TextureSlot PLATE = TextureSlot.create("plate");

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
			.texture(BRACKET, new Material(prov.modLoc("block/bracket_" + material)))
			.texture(PLATE, new Material(prov.modLoc("block/bracket_plate_" + material)))
			.build(prov.modLoc(path + "_" + material)));
	}

	public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> itemModel(String material) {
		return b -> b.model(() -> (c, p) -> {
			ModelTemplate template =
				new ModelTemplate(Optional.of(p.modLoc("block/bracket/item")), Optional.empty(), BRACKET, PLATE);
			p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
				.put(BRACKET, new Material(p.modLoc("block/bracket_" + material)))
				.put(PLATE, new Material(p.modLoc("block/bracket_plate_" + material))));
		})
			.build();
	}

}
