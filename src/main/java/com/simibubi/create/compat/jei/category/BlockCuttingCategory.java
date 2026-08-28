package com.simibubi.create.compat.jei.category;

import com.simibubi.create.foundation.recipe.RecipeAccessors;
import org.jspecify.annotations.NullMarked;
import com.simibubi.create.compat.jei.category.BlockCuttingCategory.CondensedBlockCuttingRecipe;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NullMarked
public class BlockCuttingCategory extends CreateRecipeCategory<CondensedBlockCuttingRecipe> {

	private final AnimatedSaw saw = new AnimatedSaw();

	public BlockCuttingCategory(Info<CondensedBlockCuttingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CondensedBlockCuttingRecipe recipe, IFocusGroup focuses) {
		List<List<ItemStack>> results = recipe.getCondensedOutputs();

		builder
				.addSlot(RecipeIngredientRole.INPUT, 5, 5)
				.setBackground(getRenderedSlot(), -1 , -1)
				.addItemStacks(Arrays.asList(ItemHelper.getItems(RecipeAccessors.ingredients(recipe).get(0))));

		int i = 0;
		for (List<ItemStack> itemStacks : results) {
			int xPos = 78 + (i % 5) * 19;
			int yPos = 48 + (i / 5) * -19;

			builder
					.addSlot(RecipeIngredientRole.OUTPUT, xPos, yPos)
					.setBackground(getRenderedSlot(), -1 , -1)
					.addItemStacks(itemStacks);
			i++;
		}
	}

	@Override
	public void draw(CondensedBlockCuttingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 31, 6);
		AllGuiTextures.JEI_SHADOW.render(graphics, 33 - 17, 37 + 13);
		saw.draw(graphics, 33, 37);
	}

	public static class CondensedBlockCuttingRecipe extends StonecutterRecipe {

		List<ItemStack> outputs = new ArrayList<>();

		// The recipe group moved onto StonecutterRecipe#group (hardcoded to "") and the result is now
		// an ItemStackTemplate; CommonInfo only carries the show-notification flag, which keeps its
		// former default. The category draws the condensed output list rather than this result, but
		// 26.2 will not describe a recipe as producing nothing, so the first output stands for it.
		public CondensedBlockCuttingRecipe(Ingredient ingredient, ItemStack firstOutput) {
			super(new Recipe.CommonInfo(true), ingredient, ItemStackTemplate.fromNonEmptyStack(firstOutput));
		}

		public void addOutput(ItemStack stack) {
			outputs.add(stack);
		}

		public List<ItemStack> getOutputs() {
			return outputs;
		}

		public List<List<ItemStack>> getCondensedOutputs() {
			List<List<ItemStack>> result = new ArrayList<>();
			int index = 0;
			boolean firstPass = true;
			for (ItemStack itemStack : outputs) {
				if (firstPass)
					result.add(new ArrayList<>());
				result.get(index).add(itemStack);
				index++;
				if (index >= 15) {
					index = 0;
					firstPass = false;
				}
			}
			return result;
		}

		@Override
		public boolean isSpecial() {
			return true;
		}

	}

	public static List<RecipeHolder<CondensedBlockCuttingRecipe>> condenseRecipes(List<RecipeHolder<?>> stoneCuttingRecipes) {
		List<RecipeHolder<CondensedBlockCuttingRecipe>> condensed = new ArrayList<>();
		Recipes: for (RecipeHolder<?> recipe : stoneCuttingRecipes) {
			Ingredient i1 = RecipeAccessors.ingredients(recipe.value()).get(0);
			for (RecipeHolder<CondensedBlockCuttingRecipe> condensedRecipe : condensed) {
				if (ItemHelper.matchIngredients(i1, RecipeAccessors.ingredients(condensedRecipe.value()).get(0))) {
					condensedRecipe.value().addOutput(getResultItem(recipe.value()));
					continue Recipes;
				}
			}
			ItemStack firstOutput = getResultItem(recipe.value());
			if (firstOutput.isEmpty())
				continue;
			CondensedBlockCuttingRecipe cr = new CondensedBlockCuttingRecipe(i1, firstOutput);
			cr.addOutput(firstOutput);
			condensed.add(new RecipeHolder<>(recipe.id(), cr));
		}
		return condensed;
	}

}
