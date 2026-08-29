package com.simibubi.create.content.decoration.palettes;

import net.minecraft.tags.BlockItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;

import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import net.createmod.catnip.api.lang.Lang;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
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
			.blockstate(() -> (c, p) -> generateBlockState(c, p, variantName, pattern, block))
			.recipe((c, p) -> createRecipes(variant, block, c, p))
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

	protected abstract void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
										  DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p);

	protected abstract void generateBlockState(DataGenContext<Block, B> ctx, RegistrateBlockModelGenerator prov,
											   String variantName, PaletteBlockPattern pattern,
											   Supplier<? extends Block> block);

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
		protected void generateBlockState(DataGenContext<Block, StairBlock> ctx, RegistrateBlockModelGenerator prov,
										  String variantName, PaletteBlockPattern pattern,
										  Supplier<? extends Block> block) {
			prov.generateStairsBlock(ctx.get(), new Material(getTexture(variantName, pattern, 0)));
		}

		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockItemTags.STAIRS.block());
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(BlockItemTags.STAIRS.item());
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stairs(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(p.itemLookup()
				.getOrThrow(type.materialTag)), category, c::get, 1);
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
		protected void generateBlockState(DataGenContext<Block, SlabBlock> ctx, RegistrateBlockModelGenerator prov,
										  String variantName, PaletteBlockPattern pattern,
										  Supplier<? extends Block> block) {
			Material mainTexture = new Material(getTexture(variantName, pattern, 0));
			Material sideTexture = customSide ? new Material(getTexture(variantName, pattern, 1)) : mainTexture;

			// The plain slabs borrow the pattern's own full block as their double model; the ones with a
			// side of their own need a column model built for it.
			MultiVariant doubleSlab = customSide
				? BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.create(
					prov.modLoc("block/" + ctx.getName() + "_double"),
					TextureMapping.column(sideTexture, mainTexture), prov.modelOutput))
				: BlockModelGenerators.plainVariant(prov.modLoc("block/" + pattern.createName(variantName)));

			prov.generateSlabBlock(ctx.get(), doubleSlab, sideTexture, mainTexture, mainTexture);
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
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.slab(DataIngredient.items(patternBlock.get()), category, c::get, c.getName(), false);
			p.stonecutting(DataIngredient.tag(p.itemLookup()
				.getOrThrow(type.materialTag)), category, c::get, 2);
			DataIngredient ingredient = DataIngredient.items(c.get());
			p.shapeless(category, patternBlock.get())
				.requires(ingredient.toVanilla())
				.requires(ingredient.toVanilla())
				.unlockedBy("has_" + c.getName(), ingredient.getCriterion(p))
				.save(p, Create.ID + ":" + c.getName() + "_recycling");
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
			builder.model(() -> (c, p) -> wallInventory(c.getEntry(), p, variantName, pattern));
			return super.transformItem(builder, variantName, pattern);
		}

		private void wallInventory(net.minecraft.world.item.Item item, RegistrateItemModelGenerator prov,
								   String variantName, PaletteBlockPattern pattern) {
			prov.generateWithTemplate(item,
				new ModelTemplate(Optional.of(prov.mcLoc("block/wall_inventory")), Optional.empty(), TextureSlot.WALL),
				TextureMapping.singleSlot(TextureSlot.WALL, new Material(getTexture(variantName, pattern, 0))));
		}

		@Override
		protected void generateBlockState(DataGenContext<Block, WallBlock> ctx, RegistrateBlockModelGenerator prov,
										  String variantName, PaletteBlockPattern pattern,
										  Supplier<? extends Block> block) {
			prov.generateWallBlock(ctx.get(), pattern.createName(variantName),
				new Material(getTexture(variantName, pattern, 0)));
		}


		@Override
		protected Iterable<TagKey<Block>> getBlockTags() {
			return Arrays.asList(BlockItemTags.WALLS.block());
		}

		@Override
		protected Iterable<TagKey<Item>> getItemTags() {
			return Arrays.asList(BlockItemTags.WALLS.item());
		}

		@Override
		protected void createRecipes(AllPaletteStoneTypes type, BlockEntry<? extends Block> patternBlock,
									 DataGenContext<Block, ? extends Block> c, RegistrateRecipeProvider p) {
			RecipeCategory category = RecipeCategory.BUILDING_BLOCKS;
			p.stonecutting(DataIngredient.tag(p.itemLookup()
				.getOrThrow(type.materialTag)), category, c::get, 1);
			DataIngredient ingredient = DataIngredient.items(patternBlock.get());
			p.shaped(category, c.get(), 6)
				.pattern("XXX")
				.pattern("XXX")
				.define('X', ingredient.toVanilla())
				.unlockedBy("has_" + p.safeName(ingredient), ingredient.getCriterion(p))
				// 26.2 refuses an explicit id that matches the default one, and this recipe's is the
				// block's own name either way.
				.save(p);
		}


	}

}
