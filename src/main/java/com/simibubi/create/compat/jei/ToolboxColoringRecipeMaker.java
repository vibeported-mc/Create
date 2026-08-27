package com.simibubi.create.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class ToolboxColoringRecipeMaker {

	// From JEI's ShulkerBoxColoringRecipeMaker
	public static Stream<RecipeHolder<CraftingRecipe>> createRecipes() {
		String group = "create.toolbox.color";
		ItemStack baseShulkerStack = AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.asStack();
		Ingredient baseShulkerIngredient = Ingredient.of(baseShulkerStack.getItem());

		return Arrays.stream(DyeColor.values())
			.filter(dc -> dc != DyeColor.BROWN)
			.map(color -> {
				// An ingredient is one flat set of items now, with no list of alternatives to combine.
				// The dye item is a member of its own colour tag, so naming the tag covers both halves
				// of what the old item-value/tag-value pair stood for.
				Ingredient colorIngredient = Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(color.getTag()));
				List<Ingredient> inputs = List.of(baseShulkerIngredient, colorIngredient);
				Block coloredShulkerBox = AllBlocks.TOOLBOXES.get(color)
					.get();
				ItemStackTemplate output = new ItemStackTemplate(coloredShulkerBox.asItem());
				// The recipe group and the show-notification flag moved into the two info records a
				// crafting recipe is built from.
				ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(true),
					new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, group), output, inputs);
				ResourceKey<Recipe<?>> id =
					ResourceKey.create(Registries.RECIPE, Create.asResource(group + "/" + color));
				return new RecipeHolder<CraftingRecipe>(id, recipe);
			});
	}

	private ToolboxColoringRecipeMaker() {}

}
