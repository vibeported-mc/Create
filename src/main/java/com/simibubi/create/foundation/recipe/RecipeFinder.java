package com.simibubi.create.foundation.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Looks recipes up from a {@link Level}.
 * <p>
 * Minecraft 26.2 keeps the loaded recipes on the server only - the client is sent recipe
 * <em>displays</em> for the recipe book and nothing else, and {@link Level#recipeAccess()} on the
 * client cannot answer a lookup. Every lookup Create does runs while processing on the server, so
 * these return nothing on the client rather than a stale or fabricated answer.
 */
public class RecipeFinder {

	@Nullable
	public static RecipeManager getManager(@Nullable Level level) {
		return level instanceof ServerLevel serverLevel ? serverLevel.recipeAccess() : null;
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> find(RecipeType<T> type,
		I input, @Nullable Level level) {
		RecipeManager manager = getManager(level);
		return manager == null ? Optional.empty() : manager.getRecipeFor(type, input, level);
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> all(RecipeType<T> type,
		@Nullable Level level) {
		RecipeManager manager = getManager(level);
		return manager == null ? List.of() : manager.recipeMap()
			.byType(type);
	}

	public static Collection<RecipeHolder<?>> all(@Nullable Level level) {
		RecipeManager manager = getManager(level);
		return manager == null ? List.of() : manager.recipeMap()
			.values();
	}

	private RecipeFinder() {
	}

}
