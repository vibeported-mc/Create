package com.simibubi.create.compat.jei;


import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jspecify.annotations.NullMarked;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Helper recipe type for displaying an item relationship in JEI
 */
@NullMarked
public class ConversionRecipe extends StandardProcessingRecipe<RecipeWrapper> {

	static int counter = 0;

	public static RecipeHolder<ConversionRecipe> create(ItemStack from, ItemStack to) {
		Identifier recipeId = Create.asResource("conversion_" + counter++);
		// An ingredient is a flat set of items now, so it can only name the item being converted.
		ConversionRecipe recipe = new Builder<>(ConversionRecipe::new, recipeId)
			.withItemIngredients(Ingredient.of(from.getItem()))
			.withSingleItemOutput(to)
			.build();
		// A recipe is held under a registry key rather than a bare id in 26.2. This one only exists in
		// JEI, so the key names something the recipe manager has never heard of.
		return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, recipeId), recipe);
	}

	public ConversionRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.CONVERSION, params);
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level worldIn) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

}
