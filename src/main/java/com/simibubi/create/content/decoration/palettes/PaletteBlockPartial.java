package com.simibubi.create.content.decoration.palettes;

import net.minecraft.tags.BlockItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.Arrays;
import java.util.function.Supplier;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.api.lang.Lang;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;


public abstract class PaletteBlockPartial<B extends Block> {

	public static final PaletteBlockPartial<StairBlock> STAIR = new Stairs();
	public static final PaletteBlockPartial<SlabBlock> SLAB = new Slab(false);
	public static final PaletteBlockPartial<SlabBlock> UNIQUE_SLAB = new Slab(true);
	public static final PaletteBlockPartial<WallBlock> WALL = new Wall();

	public static final PaletteBlockPartial<?>[] ALL_PARTIALS = {STAIR, SLAB, WALL};
	public static final PaletteBlockPartial<?>[] FOR_POLISHED = {STAIR, UNIQUE_SLAB, WALL};

	private String name;

	private PaletteBlockPartial(String name) {
		this.name = name;
	}

	public BlockBuilder<B, CreateRegistrate> create(String variantName, PaletteBlockPattern pattern,
																 BlockEntry<? extends Block> block, AllPaletteStoneTypes variant) {
		String patternName = Lang.nonPluralId(pattern.createName(variantName));
		String blockName = patternName + "_" + this.name;

		BlockBuilder<B, CreateRegistrate> blockBuilder = Create.registrate()
			.block(blockName, p -> createBlock(block, p))
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> createRecipes(variant, block, c, p))
			
			.transform(b -> transformBlock(b, variantName, pattern));

		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> itemBuilder = blockBuilder.item()
			.transform(b -> transformItem(b, variantName, pattern));

		if (canRecycle())
			itemBuilder.tag(variant.materialTag);

		return itemBuilder.build();
	}

	protected Identifier getTexture(String variantName, PaletteBlockPattern pattern, int index) {
		return PaletteBlockPattern.toLocation(variantName, pattern.getTexture(index));
	}

	protected BlockBuilder<B, CreateRegistrate> transformBlock(BlockBuilder<B, CreateRegistrate> builder,
															   String variantName, PaletteBlockPattern pattern) {
		getBlockTags().forEach(builder::tag);
		return builder.transform(pickaxeOnly());
	}

	protected ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> transformItem(
		ItemBuilder<BlockItem, BlockBuilder<B, CreateRegistrate>> builder, String variantName,
		PaletteBlockPattern pattern) {
		getItemTags().forEach(builder::tag);
		return builder;
	}

	protected boolean canRecycle() {
		return true;
	}

	protected abstract Iterable<TagKey<Block>> getBlockTags();

	protected abstract Iterable<TagKey<Item>> getItemTags();

	/**
	 * @param properties the properties Registrate prepared for this partial - only its registry id is
	 *                   kept, since the rest of the block's character is copied from the base block.
	 *                   26.2 refuses to construct a block whose properties carry no id.
	 */
	protected abstract B createBlock(Supplier<? extends Block> block, Properties properties);

	protected static Properties copyOf(Block block, Properties properties) {
		return Properties.ofFullCopy(block).setId(properties.id);
	}



	private static class Stairs extends PaletteBlockPartial<StairBlock> {

		public Stairs() {
			super("stairs");
		}

		@Override
		protected StairBlock createBlock(Supplier<? extends Block> block, Properties properties) {
			return new StairBlock(block.get().defaultBlockState(), copyOf(block.get(), properties));
		}


		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockItemTags.STAIRS.block());
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(BlockItemTags.STAIRS.item());
		}


	}

	private static class Slab extends PaletteBlockPartial<SlabBlock> {

		private boolean customSide;

		public Slab(boolean customSide) {
			super("slab");
			this.customSide = customSide;
		}

		@Override
		protected SlabBlock createBlock(Supplier<? extends Block> block, Properties properties) {
			return new SlabBlock(copyOf(block.get(), properties));
		}

		@Override
		protected boolean canRecycle() {
			return false;
		}


		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockItemTags.SLABS.block());
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(BlockItemTags.SLABS.item());
		}


		@Override
		protected BlockBuilder<SlabBlock, CreateRegistrate> transformBlock(
			BlockBuilder<SlabBlock, CreateRegistrate> builder, String variantName, PaletteBlockPattern pattern) {
			builder.loot((lt, block) -> lt.add(block, lt.createSlabItemTable(block)));
			return super.transformBlock(builder, variantName, pattern);
		}

	}

	private static class Wall extends PaletteBlockPartial<WallBlock> {

		public Wall() {
			super("wall");
		}

		@Override
		protected WallBlock createBlock(Supplier<? extends Block> block, Properties properties) {
			return new WallBlock(copyOf(block.get(), properties).forceSolidOn());
		}

		@Override
		protected ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> transformItem(
			ItemBuilder<BlockItem, BlockBuilder<WallBlock, CreateRegistrate>> builder, String variantName,
			PaletteBlockPattern pattern) {
			// datagen: builder/* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			return super.transformItem(builder, variantName, pattern);
		}


		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockItemTags.WALLS.block());
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(BlockItemTags.WALLS.item());
		}


	}

}
