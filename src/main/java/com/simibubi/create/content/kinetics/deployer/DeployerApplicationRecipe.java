package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.RecipeAccessors;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

public class DeployerApplicationRecipe extends ItemApplicationRecipe implements IAssemblyRecipe {

	public DeployerApplicationRecipe(ItemApplicationRecipeParams params) {
		super(AllRecipeTypes.DEPLOYING, params);
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	public static RecipeHolder<DeployerApplicationRecipe> convert(RecipeHolder<?> sandpaperRecipe) {
		Identifier sourceId = sandpaperRecipe.id()
			.identifier();
		Identifier id = sourceId.withSuffix("_using_deployer");
		DeployerApplicationRecipe recipe = new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, id)
				.require(RecipeAccessors.ingredients(sandpaperRecipe.value())
						.get(0))
						.require(AllItemTags.SANDPAPER.tag)
						.output(RecipeAccessors.result(sandpaperRecipe.value(), Minecraft.getInstance().level))
						.build();

		return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe);
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {
		list.add(ingredients.get(1));
	}

	@Override
	public Component getDescriptionForAssembly() {
		ItemStack[] matchingStacks = ItemHelper.getItems(ingredients.get(1));
		if (matchingStacks.length == 0) {
            return Component.literal("Invalid");
        }
		return CreateLang.translateDirect("recipe.assembly.deploying_item",
			matchingStacks[0].getHoverName().getString());
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.DEPLOYER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> SequencedAssemblySubCategory.AssemblyDeploying::new;
	}

}
