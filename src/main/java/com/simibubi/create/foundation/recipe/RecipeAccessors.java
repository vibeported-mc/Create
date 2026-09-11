package com.simibubi.create.foundation.recipe;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/**
 * Reads a recipe's ingredients and result whatever kind of recipe it is.
 * <p>
 * Minecraft 26.2 took getIngredients and getResultItem off the Recipe interface: what a recipe needs
 * is described by its {@link net.minecraft.world.item.crafting.PlacementInfo}, and what it produces
 * by its {@link RecipeDisplay}. Create's own processing recipes still carry both directly, so these
 * prefer them and fall back to the vanilla description.
 */
public class RecipeAccessors {

	public static List<Ingredient> ingredients(Recipe<?> recipe) {
		if (recipe instanceof ProcessingRecipe<?, ?> processing)
			return processing.getIngredients();

		// Deliberately not placeable, which keeps a 5x5 grid out of the 3x3 recipe book - and leaves
		// its placement info with no ingredients at all.
		if (recipe instanceof MechanicalCraftingRecipe mechanical)
			return mechanical.getIngredients()
				.stream()
				.flatMap(Optional::stream)
				.toList();
		return recipe.placementInfo()
			.ingredients();
	}

	public static ItemStack result(Recipe<?> recipe, @Nullable Level level) {
		if (recipe instanceof ProcessingRecipe<?, ?> processing)
			return processing.getResultItem(null);

		// Not a processing recipe, and it describes no display either - it keeps its outcomes in a
		// weighted pool of its own. Left to the fallback below it resolves to nothing, which is how
		// every sequenced assembly recipe lost its output slot in JEI and could not be found with R.
		if (recipe instanceof SequencedAssemblyRecipe sequenced)
			return sequenced.getResultItem(null);

		// No vanilla display either, for the same reason it is not placeable.
		if (recipe instanceof MechanicalCraftingRecipe mechanical)
			return mechanical.getResultItem();

		// A display's result is resolved against the level it would be crafted in, so without one there
		// is nothing to resolve it from.
		if (level == null)
			return ItemStack.EMPTY;

		ContextMap context = SlotDisplayContext.fromLevel(level);
		for (RecipeDisplay display : recipe.display()) {
			ItemStack stack = display.result()
				.resolveForFirstStack(context);
			if (!stack.isEmpty())
				return stack;
		}
		return ItemStack.EMPTY;
	}

	private RecipeAccessors() {
	}

}
