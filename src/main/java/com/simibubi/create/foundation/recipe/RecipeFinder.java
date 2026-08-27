package com.simibubi.create.foundation.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.Create;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Utility for searching through a level's recipe collection.
 * Non-dynamic conditions can be split off into an initial search for caching intermediate results.
 * <p>
 * Minecraft 26.2 keeps the loaded recipes on the server; the client is sent only the recipe book's
 * displays. NeoForge sends back the types a mod asks for on datapack sync, which is what
 * {@link ClientRecipes} holds - so this answers from the server's recipe manager or from what was
 * synced, depending on which side is asking.
 *
 * @author simibubi
 */
public class RecipeFinder {
	private static final Cache<Object, List<RecipeHolder<? extends Recipe<?>>>> CACHED_SEARCHES = CacheBuilder.newBuilder().build();

	public static final ResourceManagerReloadListener LISTENER = resourceManager -> CACHED_SEARCHES.invalidateAll();

	/**
	 * Find all recipes matching the condition predicate.
	 * If this search is made more than once,
	 * using the same object instance as the cacheKey will retrieve the cached result from the first search.
	 *
	 * @param cacheKey (can be null to prevent the caching)
	 * @return A started search to continue with more specific conditions.
	 */
	public static List<RecipeHolder<? extends Recipe<?>>> get(@Nullable Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) {
		if (cacheKey == null)
			return startSearch(level, conditions);

		try {
			return CACHED_SEARCHES.get(cacheKey, () -> startSearch(level, conditions));
		} catch (ExecutionException e) {
			Create.LOGGER.error("Encountered a exception while searching for recipes", e);
		}

		return Collections.emptyList();
	}

	private static List<RecipeHolder<? extends Recipe<?>>> startSearch(Level level, Predicate<? super RecipeHolder<? extends Recipe<?>>> conditions) {
		List<RecipeHolder<? extends Recipe<?>>> recipes = new ArrayList<>();
		for (RecipeHolder<? extends Recipe<?>> r : all(level))
			if (conditions.test(r))
				recipes.add(r);
		return recipes;
	}

	/**
	 * The server's recipe manager, or null when asking from the client.
	 */
	@Nullable
	public static RecipeManager getManager(@Nullable Level level) {
		return level instanceof ServerLevel serverLevel ? serverLevel.recipeAccess() : null;
	}

	private static RecipeMap map(@Nullable Level level) {
		RecipeManager manager = getManager(level);
		if (manager != null)
			return manager.recipeMap();
		return FMLLoader.getCurrent().getDist() == Dist.CLIENT ? ClientRecipes.get() : RecipeMap.EMPTY;
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> find(RecipeType<T> type,
		I input, @Nullable Level level) {
		return matching(type, input, level).stream()
			.findFirst();
	}

	/**
	 * Every recipe of a type that matches the given input, best first.
	 */
	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> matching(RecipeType<T> type,
		I input, @Nullable Level level) {
		return map(level).getRecipesFor(type, input, level)
			.toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> all(RecipeType<T> type,
		@Nullable Level level) {
		return map(level).byType(type);
	}

	public static Collection<RecipeHolder<?>> all(@Nullable Level level) {
		return map(level).values();
	}

	/**
	 * The recipe with a given key, if it is loaded.
	 */
	public static Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> id, @Nullable Level level) {
		return Optional.ofNullable(map(level).byKey(id));
	}

}
