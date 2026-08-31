package com.simibubi.create.content.fluids.transfer;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;

import net.createmod.catnip.api.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
public class GenericItemEmptying {

	public static boolean canItemBeEmptied(Level world, ItemStack stack) {
		if (PotionFluidHandler.isPotionItem(stack))
			return true;

		if (AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), world)
			.isPresent())
			return true;

		ResourceHandler<FluidResource> capability = Capabilities.Fluid.ITEM.getCapability(stack, ItemAccess.forStack(stack));
		if (capability == null)
			return false;
		for (int i = 0; i < capability.size(); i++) {
			if (FluidUtil.getStack(capability, i)
				.getAmount() > 0)
				return true;
		}
		return false;
	}

	public static Pair<FluidStack, ItemStack> emptyItem(Level level, ItemStack stack, boolean simulate) {
		FluidStack resultingFluid = FluidStack.EMPTY;
		ItemStack resultingItem = ItemStack.EMPTY;

		if (PotionFluidHandler.isPotionItem(stack))
			return PotionFluidHandler.emptyPotion(stack, simulate);

		Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe = AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), level);
		if (recipe.isPresent()) {
			EmptyingRecipe emptyingRecipe = (EmptyingRecipe) recipe.get().value();
			List<ItemStack> results = emptyingRecipe.rollResults(level.getRandom());
			if (!simulate)
				stack.shrink(1);
			resultingItem = results.isEmpty() ? ItemStack.EMPTY : results.get(0);
			resultingFluid = emptyingRecipe.getResultingFluid();
			return Pair.of(resultingFluid, resultingItem);
		}

		ItemStack split = stack.copy();
		split.setCount(1);
		// One slot the fluid handler is allowed to swap the item inside: filling a bucket
		// changes which item is held, and an access over a bare stack refuses that.
		ItemStacksResourceHandler carrier = new ItemStacksResourceHandler(1);
		carrier.set(0, ItemResource.of(split), split.getCount());
		ItemAccess access = ItemAccess.forHandlerIndex(carrier, 0);
		ResourceHandler<FluidResource> capability = access.getCapability(Capabilities.Fluid.ITEM);
		if (capability == null)
			return Pair.of(resultingFluid, resultingItem);
		try (Transaction transaction = Transaction.openRoot()) {
			ResourceStack<FluidResource> transferred = ResourceHandlerUtil.extractFirst(capability, resource -> true, 1000, transaction);
			resultingFluid = transferred == null ? FluidStack.EMPTY : transferred.resource().toStack(transferred.amount());
			if (!simulate)
				transaction.commit();
		}
		resultingItem = ItemUtil.getStack(carrier, 0)
			.copy();
		if (!simulate)
			stack.shrink(1);

		return Pair.of(resultingFluid, resultingItem);
	}

}
