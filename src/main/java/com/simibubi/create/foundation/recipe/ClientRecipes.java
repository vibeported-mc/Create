package com.simibubi.create.foundation.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Create's recipes, as the client sees them.
 * <p>
 * Minecraft 26.2 stopped sending the loaded recipes to clients - only the recipe book's displays are
 * synced. NeoForge kept a way to ask for them back: a mod names the recipe types it needs on datapack
 * sync, and the client is handed those and nothing else. Create's own types are asked for, so the
 * recipe lookups that run on the client - JEI, the blueprint overlay, the factory panel's crafting
 * preview - see the same recipes the server does.
 */
public class ClientRecipes {

	private static RecipeMap recipes = RecipeMap.EMPTY;

	public static void receive(RecipeMap recipeMap) {
		recipes = recipeMap;
	}

	public static void clear() {
		recipes = RecipeMap.EMPTY;
	}

	public static RecipeMap get() {
		return recipes;
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> all(RecipeType<T> type) {
		return recipes.byType(type);
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> matching(RecipeType<T> type,
		I input, @Nullable Level level) {
		return recipes.getRecipesFor(type, input, level)
			.toList();
	}

	public static Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> id) {
		return Optional.ofNullable(recipes.byKey(id));
	}

	public static Collection<RecipeHolder<?>> values() {
		return recipes.values();
	}

	private ClientRecipes() {
	}

}
