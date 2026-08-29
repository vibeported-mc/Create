package com.simibubi.create.foundation.data.recipe;


import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.PolishingRecipeGen;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeOutput;

/**
 * Create's own Data Generation for the singular default polishing recipe
 * @see PolishingRecipeGen
 */
@SuppressWarnings("unused")
public final class CreatePolishingRecipeGen extends PolishingRecipeGen {

	GeneratedRecipe

	ROSE_QUARTZ = create(AllItems.ROSE_QUARTZ::get, b -> b.output(AllItems.POLISHED_ROSE_QUARTZ.get()))

	;

	public CreatePolishingRecipeGen(Provider registries, RecipeOutput output) {
		super(registries, output, Create.ID);
	}
}
