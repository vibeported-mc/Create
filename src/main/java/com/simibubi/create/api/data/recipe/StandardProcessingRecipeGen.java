package com.simibubi.create.api.data.recipe;


import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;

/**
 * A base class for {@link StandardProcessingRecipe}, containing helper methods
 * for datagenning processing recipes.
 * <p>
 * Addons should extend this for custom processing recipe that extends {@link StandardProcessingRecipe},
 * and return the recipe type in {@link #getRecipeType()}.
 */
public abstract class StandardProcessingRecipeGen<R extends StandardProcessingRecipe<?>> extends ProcessingRecipeGen<ProcessingRecipeParams, R, StandardProcessingRecipe.Builder<R>> {
	public StandardProcessingRecipeGen(Provider registries, RecipeOutput output, String defaultNamespace) {
		super(registries, output, defaultNamespace);
	}

	@SuppressWarnings("unchecked")
	protected StandardProcessingRecipe.Serializer<R> getSerializer() {
		return (StandardProcessingRecipe.Serializer<R>) getRecipeType().getProcessingSerializer();
	}

	@Override
	protected Builder<R> getBuilder(Identifier id) {
		return new StandardProcessingRecipe.Builder<>(getSerializer().factory(), id);
	}
}
