package com.simibubi.create.api.data.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.Create;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A class containing some basic setup for other recipe generators to use.
 * Addons should extend this if they add a custom recipe type that is not
 * a processing recipe type and want to use Create's helpers.
 * For processing recipes extend {@link StandardProcessingRecipeGen}.
 */
public abstract class BaseRecipeProvider extends RecipeProvider {
	private static boolean boundComponents;
	protected final String modid;
	protected final List<GeneratedRecipe> all = new ArrayList<>();

	public BaseRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, String defaultNamespace) {
		super(registries, output);
		this.modid = defaultNamespace;
		bindItemComponents(registries);
	}

	protected Identifier asResource(String path) {
		return Identifier.fromNamespaceAndPath(modid, path);
	}

	/**
	 * An item's default components are bound when a server loads its datapacks, which never happens
	 * during datagen - so anything that asks an item about itself fails on "Components not bound yet".
	 * Bind them from the registries the provider was handed.
	 * <p>
	 * One wrinkle: those registries answer a tag lookup with an unbound placeholder, an anonymous
	 * subclass of HolderSet.Named. NeoForge only whitelists Named itself as a component value, so say
	 * up front that the placeholder is fine too.
	 */
	public static void bindItemComponents(HolderLookup.Provider registries) {
		if (boundComponents)
			return;
		boundComponents = true;

		CommonHooks.markComponentClassAsValid(HolderSet.emptyNamed(new HolderOwner<>() {
		}, TagKey.create(Registries.ITEM, Create.asResource("datagen_placeholder")))
			.getClass());

		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
			.forEach(DataComponentInitializers.PendingComponents::apply);
	}

	/**
	 * 26.2's Ingredient wraps an already-resolved set of items, so a tag has to be looked up on a
	 * getter that has tags bound - the provider's own - rather than named on the ingredient.
	 */
	protected Ingredient ingredient(TagKey<Item> tag) {
		return Ingredient.of(items.getOrThrow(tag));
	}

	protected GeneratedRecipe register(GeneratedRecipe recipe) {
		all.add(recipe);
		return recipe;
	}

	@Override
	public void buildRecipes() {
		all.forEach(c -> c.register(output));
		Create.LOGGER.info("{} registered {} recipe{}", getName(), all.size(), all.size() == 1 ? "" : "s");
	}

	/**
	 * A name for the log line above. 26.2 moved getName onto the DataProvider half, so a generator
	 * that wants a prettier name says so here.
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * The DataProvider half. 26.2 builds the recipe provider itself once the registries have loaded,
	 * handing it the output to write into, so the generator can no longer be a DataProvider directly.
	 */
	public static DataProvider runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
		String name, BiFunction<HolderLookup.Provider, RecipeOutput, RecipeProvider> factory) {
		return new RecipeProvider.Runner(output, registries) {
			@Override
			protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput recipeOutput) {
				return factory.apply(lookup, recipeOutput);
			}

			@Override
			public String getName() {
				return name;
			}
		};
	}

	@FunctionalInterface
	public interface GeneratedRecipe {
		void register(RecipeOutput recipeOutput);
	}
}
