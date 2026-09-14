package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.foundation.fluid.FluidHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class FillingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {

	public FillingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.FILLING, params);
	}

	@Override
	public boolean matches(SingleRecipeInput inv, Level p_77569_2_) {
		return ingredients.get(0)
			.test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	public SizedFluidIngredient getRequiredFluid() {
		if (fluidIngredients.isEmpty())
			throw new IllegalStateException("Filling Recipe has no fluid ingredient!");
		return fluidIngredients.get(0);
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	public void addAssemblyFluidIngredients(List<SizedFluidIngredient> list) {
		list.add(getRequiredFluid());
	}

	@Override
	public Component getDescriptionForAssembly() {
		// Whole stacks, so a potion step is named after its potion rather than after the bare fluid.
		List<FluidStack> matchingFluids = FluidHelper.matchingStacks(fluidIngredients.get(0), null);
		if (matchingFluids.isEmpty()) {
            return Component.literal("Invalid");
        }
		return CreateLang.translateDirect("recipe.assembly.spout_filling_fluid",
			matchingFluids.get(0).getHoverName()
				.getString());
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.SPOUT.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> SequencedAssemblySubCategory.AssemblySpouting::new;
	}

}
