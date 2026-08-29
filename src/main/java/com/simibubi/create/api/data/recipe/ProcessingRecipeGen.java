package com.simibubi.create.api.data.recipe;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * A base class for all processing recipes, containing helper methods
 * for datagenning processing recipes.
 * <p>
 * Addons should usually extend {@link StandardProcessingRecipeGen} instead if the processing recipe uses
 * the base {@link ProcessingRecipeParams}.
 * For processing recipes that uses <b>CUSTOM</b> {@link ProcessingRecipeParams} like {@link ItemApplicationRecipe},
 * extend this class and override {@link #getRecipeType()} and {@link #getBuilder(Identifier)},
 * returning the corresponding recipe type and recipe builder.
 */
public abstract class ProcessingRecipeGen<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>, B extends ProcessingRecipeBuilder<P, R, B>> extends BaseRecipeProvider {

	public ProcessingRecipeGen(HolderLookup.Provider registries, RecipeOutput output, String defaultNamespace) {
		super(registries, output, defaultNamespace);
	}

	/**
	 * Create a processing recipe with a single itemstack ingredient, using its id
	 * as the name of the recipe
	 */
	protected GeneratedRecipe create(String namespace, Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
		GeneratedRecipe generatedRecipe = c -> {
			ItemLike itemLike = singleIngredient.get();
			transform
				.apply(builder(Identifier.fromNamespaceAndPath(namespace, RegisteredObjectsHelper.getKeyOrThrow(itemLike.asItem()).getPath())).withItemIngredients(Ingredient.of(itemLike)))
				.build(c);
		};
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	/**
	 * Create a processing recipe with a single itemstack ingredient, using its id
	 * as the name of the recipe
	 */
	protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) {
		return create(Create.ID, singleIngredient, transform);
	}

	protected GeneratedRecipe createWithDeferredId(Supplier<Identifier> name, UnaryOperator<B> transform) {
		GeneratedRecipe generatedRecipe =
			c -> transform.apply(builder(name.get()))
				.build(c);
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	/**
	 * Create a new processing recipe, with recipe definitions provided by the
	 * function
	 */
	protected GeneratedRecipe create(Identifier name, UnaryOperator<B> transform) {
		return createWithDeferredId(() -> name, transform);
	}

	/**
	 * Create a new processing recipe, with recipe definitions provided by the
	 * function, under the default namespace
	 */
	protected GeneratedRecipe create(String name, UnaryOperator<B> transform) {
		return create(asResource(name), transform);
	}

	protected abstract IRecipeTypeInfo getRecipeType();

	protected abstract B getBuilder(Identifier id);

	/**
	 * The builder a recipe is written with, already told where to resolve tags: only the lookup this
	 * provider was handed has them bound while datagen runs.
	 */
	private B builder(Identifier id) {
		return getBuilder(id).withItemLookup(items)
			.withFluidLookup(registries.lookupOrThrow(Registries.FLUID));
	}

	protected Supplier<Identifier> idWithSuffix(Supplier<ItemLike> item, String suffix) {
		return () -> {
			Identifier registryName = RegisteredObjectsHelper.getKeyOrThrow(item.get()
					.asItem());
			return asResource(registryName.getPath() + suffix);
		};
	}

	/**
	 * Gets a display name for this recipe generator.
	 * It is recommended to override this for a prettier name, however that is not
	 * required.
	 */
	@NotNull
	@Override
	public String getName() {
		return modid + "'s processing recipes: " + getRecipeType().getId()
			.getPath();
	}

}
