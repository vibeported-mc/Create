package com.simibubi.create.foundation.data;

import com.simibubi.create.Create;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public class ModelGen {

	private static final TextureSlot OVERLAY = TextureSlot.create("overlay");

	public static Identifier createOvergrown(DataGenContext<Block, ? extends Block> ctx,
		RegistrateBlockModelGenerator prov, Identifier block, Identifier overlay) {
		return createOvergrown(ctx, prov, block, block, block, overlay);
	}

	public static Identifier createOvergrown(DataGenContext<Block, ? extends Block> ctx,
		RegistrateBlockModelGenerator prov, Identifier side, Identifier top, Identifier bottom, Identifier overlay) {
		return prov.getBuilder()
			.parent(Create.asResource("block/overgrown"))
			.texture(TextureSlot.PARTICLE, new Material(side))
			.texture(TextureSlot.SIDE, new Material(side))
			.texture(TextureSlot.TOP, new Material(top))
			.texture(TextureSlot.BOTTOM, new Material(bottom))
			.texture(OVERLAY, new Material(overlay))
			.build(prov.modLoc("block/" + ctx.getName()));
	}

	public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel() {
		return b -> b.model(() -> AssetLookup::customItemModel)
			.build();
	}

	public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel(String... path) {
		return b -> b.model(() -> AssetLookup.customBlockItemModel(path))
			.build();
	}

}
