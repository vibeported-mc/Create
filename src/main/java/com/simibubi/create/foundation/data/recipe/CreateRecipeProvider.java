package com.simibubi.create.foundation.data.recipe;

import net.minecraft.tags.BlockItemTags;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * The class that handles gathering Create's generated recipes for most types.
 * Data here is only generated when running server datagen
 *
 * @see com.simibubi.create.infrastructure.data.CreateDatagen
 */
/**
 * 26.2 splits a recipe provider in two: the DataProvider half waits on the registries, then builds
 * the provider proper with the output already in hand. So the processing generators are gathered
 * behind one DataProvider that builds all of them per run rather than each being one itself.
 */
public final class CreateRecipeProvider {

	static final int BUCKET = FluidType.BUCKET_VOLUME;
	static final int BOTTLE = 250;

	private CreateRecipeProvider() {
	}

	public static DataProvider allProcessing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		return BaseRecipeProvider.runner(output, registries, "Create's Processing Recipes", AllProcessing::new);
	}

	private static class AllProcessing extends RecipeProvider {

		private final List<BaseRecipeProvider> generators;

		private AllProcessing(HolderLookup.Provider registries, RecipeOutput output) {
			super(registries, output);
			generators = List.of(new CreateCrushingRecipeGen(registries, output),
				new CreateMillingRecipeGen(registries, output), new CreateCuttingRecipeGen(registries, output),
				new CreateWashingRecipeGen(registries, output), new CreatePolishingRecipeGen(registries, output),
				new CreateDeployingRecipeGen(registries, output), new CreateMixingRecipeGen(registries, output),
				new CreateCompactingRecipeGen(registries, output), new CreatePressingRecipeGen(registries, output),
				new CreateFillingRecipeGen(registries, output), new CreateEmptyingRecipeGen(registries, output),
				new CreateHauntingRecipeGen(registries, output),
				new CreateItemApplicationRecipeGen(registries, output));
		}

		@Override
		protected void buildRecipes() {
			generators.forEach(BaseRecipeProvider::buildRecipes);
		}
	}

	protected static class I {

		static TagKey<Item> redstone() {
			return Tags.Items.DUSTS_REDSTONE;
		}

		static TagKey<Item> planks() {
			return ItemTags.PLANKS;
		}

		static TagKey<Item> woodSlab() {
			return BlockItemTags.WOODEN_SLABS.item();
		}

		static TagKey<Item> gold() {
			return Tags.Items.INGOTS_GOLD;
		}

		static TagKey<Item> goldSheet() {
			return CommonMetal.GOLD.plates;
		}

		static TagKey<Item> stone() {
			return Tags.Items.STONES;
		}

		static ItemLike andesiteAlloy() {
			return AllItems.ANDESITE_ALLOY.get();
		}

		static ItemLike shaft() {
			return AllBlocks.SHAFT.get();
		}

		static ItemLike cog() {
			return AllBlocks.COGWHEEL.get();
		}

		static ItemLike largeCog() {
			return AllBlocks.LARGE_COGWHEEL.get();
		}

		static ItemLike andesiteCasing() {
			return AllBlocks.ANDESITE_CASING.get();
		}

		static ItemLike vault() {
			return AllBlocks.ITEM_VAULT.get();
		}

		static ItemLike stockLink() {
			return AllBlocks.STOCK_LINK.get();
		}

		static TagKey<Item> brass() {
			return CommonMetal.BRASS.ingots;
		}

		static TagKey<Item> brassSheet() {
			return CommonMetal.BRASS.plates;
		}

		static TagKey<Item> iron() {
			return Tags.Items.INGOTS_IRON;
		}

		static TagKey<Item> ironNugget() {
			return Tags.Items.NUGGETS_IRON;
		}

		static TagKey<Item> zinc() {
			return CommonMetal.ZINC.ingots;
		}

		static TagKey<Item> ironSheet() {
			return CommonMetal.IRON.plates;
		}

		static TagKey<Item> sturdySheet() {
			return AllItemTags.OBSIDIAN_PLATES.tag;
		}

		static ItemLike brassCasing() {
			return AllBlocks.BRASS_CASING.get();
		}

		static ItemLike cardboard() {
			return AllItems.CARDBOARD.get();
		}

		static ItemLike railwayCasing() {
			return AllBlocks.RAILWAY_CASING.get();
		}

		static ItemLike electronTube() {
			return AllItems.ELECTRON_TUBE.get();
		}

		static ItemLike precisionMechanism() {
			return AllItems.PRECISION_MECHANISM.get();
		}

		static TagKey<Item> brassBlock() {
			return CommonMetal.BRASS.storageBlocks.items();
		}

		static TagKey<Item> zincBlock() {
			return CommonMetal.ZINC.storageBlocks.items();
		}

		static TagKey<Item> wheatFlour() {
			return AllItemTags.WHEAT_FLOURS.tag;
		}

		static TagKey<Item> copper() {
			return Tags.Items.INGOTS_COPPER;
		}

		static TagKey<Item> copperNugget() {
			return CommonMetal.COPPER.nuggets;
		}

		static TagKey<Item> copperBlock() {
			return Tags.Items.STORAGE_BLOCKS_COPPER;
		}

		static TagKey<Item> copperSheet() {
			return CommonMetal.COPPER.plates;
		}

		static TagKey<Item> brassNugget() {
			return CommonMetal.BRASS.nuggets;
		}

		static TagKey<Item> zincNugget() {
			return CommonMetal.ZINC.nuggets;
		}

		static ItemLike copperCasing() {
			return AllBlocks.COPPER_CASING.get();
		}

		static ItemLike refinedRadiance() {
			return AllItems.REFINED_RADIANCE.get();
		}

		static ItemLike shadowSteel() {
			return AllItems.SHADOW_STEEL.get();
		}

		static TagKey<Item> netherite() {
			return Tags.Items.INGOTS_NETHERITE;
		}

	}
}
