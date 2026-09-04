package com.simibubi.create.foundation.data;

import java.util.function.Function;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Points at models that are authored by hand rather than generated.
 * <p>
 * 26.2 has no {@code ModelFile} and no existence checking to go with it: a blockstate refers to a
 * model purely by id, and the id is wrapped into a {@link MultiVariant} to be used as a variant.
 * These helpers therefore hand back ids and variants, and a missing file shows up when the game
 * loads the pack rather than while it is being generated.
 */
public class AssetLookup {

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot INDICATOR = TextureSlot.create("indicator");
	}


	/**
	 * Custom block models packaged with other partials. Example:
	 * models/block/schematicannon/block.json <br>
	 * <br>
	 * Adding "powered", "vertical" will look for /block_powered_vertical.json
	 */
	public static Identifier partialBaseModel(DataGenContext<?, ?> ctx, RegistrateBlockModelGenerator prov,
		String... suffix) {
		String string = "/block";
		for (String suf : suffix)
			if (!suf.isEmpty())
				string += "_" + suf;
		return prov.modLoc("block/" + ctx.getName() + string);
	}

	public static MultiVariant partialBaseVariant(DataGenContext<?, ?> ctx, RegistrateBlockModelGenerator prov,
		String... suffix) {
		return BlockModelGenerators.plainVariant(partialBaseModel(ctx, prov, suffix));
	}

	/**
	 * Custom block model from models/block/x.json
	 */
	public static Identifier standardModel(DataGenContext<?, ?> ctx, RegistrateBlockModelGenerator prov) {
		return prov.modLoc("block/" + ctx.getName());
	}

	public static MultiVariant standardVariant(DataGenContext<?, ?> ctx, RegistrateBlockModelGenerator prov) {
		return BlockModelGenerators.plainVariant(standardModel(ctx, prov));
	}

	/**
	 * Generate item model inheriting from a seperate model in
	 * models/block/x/item.json
	 */
	public static <I extends BlockItem> void customItemModel(DataGenContext<Item, I> ctx,
		RegistrateItemModelGenerator prov) {
		prov.generateBlockItem(ctx.getEntry(), "/item");
	}

	/**
	 * Generate item model inheriting from a seperate model in
	 * models/block/folders[0]/folders[1]/.../item.json "_" will be replaced by the
	 * item name
	 */
	public static <I extends BlockItem> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelGenerator> customBlockItemModel(
		String... folders) {
		return (c, p) -> {
			String path = "block";
			for (String string : folders)
				path += "/" + ("_".equals(string) ? c.getName() : string);
			p.createWithExistingModel(c.getEntry(), p.modLoc(path));
		};
	}

	public static <I extends Item> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelGenerator> customGenericItemModel(
		String... folders) {
		return (c, p) -> {
			String path = "block";
			for (String string : folders)
				path += "/" + ("_".equals(string) ? c.getName() : string);
			p.createWithExistingModel(c.getEntry(), p.modLoc(path));
		};
	}

	public static Function<BlockState, MultiVariant> forPowered(DataGenContext<?, ?> ctx,
		RegistrateBlockModelGenerator prov) {
		return state -> state.getValue(BlockStateProperties.POWERED) ? partialBaseVariant(ctx, prov, "powered")
			: partialBaseVariant(ctx, prov);
	}

	public static Function<BlockState, MultiVariant> forPowered(DataGenContext<?, ?> ctx,
		RegistrateBlockModelGenerator prov, String path) {
		return state -> BlockModelGenerators.plainVariant(
			prov.modLoc("block/" + path + (state.getValue(BlockStateProperties.POWERED) ? "_powered" : "")));
	}

	/**
	 * The indicator variants are models in their own right - the base model with one texture swapped
	 * - so unlike the lookups above this one writes a model out as a side effect.
	 */
	public static Function<BlockState, MultiVariant> withIndicator(DataGenContext<?, ?> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> baseModelFunc,
		IntegerProperty property) {
		return state -> {
			Identifier baseModel = baseModelFunc.apply(state)
				.variants()
				.unwrap()
				.getFirst()
				.value()
				.modelLocation();
			Integer integer = state.getValue(property);
			return BlockModelGenerators.plainVariant(prov.getBuilder()
				.parent(baseModel)
				.texture(Client.INDICATOR, prov.modBlockTexture("indicator/" + integer))
				.build(prov.modLoc("block/" + ctx.getName() + "_" + integer)));
		};
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelGenerator> existingItemModel() {
		return (c, p) -> p.createWithExistingModel(c.getEntry(), p.modLoc("item/" + c.getName()));
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelGenerator> itemModel(String name) {
		return (c, p) -> p.createWithExistingModel(c.getEntry(), p.modLoc("item/" + name));
	}

	public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelGenerator> itemModelWithPartials() {
		return (c, p) -> p.createWithExistingModel(c.getEntry(), p.modLoc("item/" + c.getName() + "/item"));
	}

}
