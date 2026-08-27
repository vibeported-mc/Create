package com.simibubi.create.compat.jei.category;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import com.simibubi.create.foundation.fluid.FluidHandlerHelpers;
import com.simibubi.create.foundation.fluid.ItemFluidAccess;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NullMarked;
import java.util.Collection;
import java.util.function.Consumer;


import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

@NullMarked
public class SpoutCategory extends CreateRecipeCategory<FillingRecipe> {

	private final AnimatedSpout spout = new AnimatedSpout();

	public SpoutCategory(Info<FillingRecipe> info) {
		super(info);
	}

	public static void consumeRecipes(Consumer<RecipeHolder<FillingRecipe>> consumer, IIngredientManager ingredientManager) {
		Collection<FluidStack> fluidStacks = ingredientManager.getAllIngredients(NeoForgeTypes.FLUID_STACK);
		for (ItemStack stack : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
			if (PotionFluidHandler.isPotionItem(stack)) {
				FluidStack fluidFromPotionItem = PotionFluidHandler.getFluidFromPotionItem(stack);
				Ingredient bottle = Ingredient.of(Items.GLASS_BOTTLE);
				Identifier id = Create.asResource("potions");
				SizedFluidIngredient fluidIngredient = new SizedFluidIngredient(
					DataComponentFluidIngredient.of(false, fluidFromPotionItem), fluidFromPotionItem.getAmount());
				FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
						.withItemIngredients(bottle)
					.withFluidIngredients(fluidIngredient)
						.withSingleItemOutput(stack)
						.build();
				consumer.accept(new RecipeHolder<>(recipeKey(id), recipe));
				continue;
			}

			ResourceHandler<FluidResource> capability = Capabilities.Fluid.ITEM.getCapability(stack, ItemAccess.forStack(stack));
			if (capability == null)
				continue;

			int numTanks = capability.size();
			FluidStack existingFluid = numTanks == 1 ? FluidHandlerHelpers.getFluidInTank(capability, 0) : FluidStack.EMPTY;

			for (FluidStack fluidStack : fluidStacks) {
				// Hoist the fluid equality check to avoid the work of copying the stack + populating capabilities
				// when most fluids will not match
				if (numTanks == 1 && (!existingFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(existingFluid, fluidStack)))
					continue;

				ItemStack copy = stack.copy();
				// Filling a bucket swaps the item out from under the handler, which an access over a bare
				// stack refuses to do; the filled item is read back out of the access afterwards rather
				// than being handed over as a container.
				ItemFluidAccess access = new ItemFluidAccess(copy);
				ResourceHandler<FluidResource> fhi = access.handler();
				if (fhi != null) {
					if (!GenericItemFilling.isFluidHandlerValid(copy, fhi))
						continue;
					FluidStack fluidCopy = fluidStack.copy();
					fluidCopy.setAmount(1000);
					FluidHandlerHelpers.fill(fhi, fluidCopy, false);
					ItemStack container = access.result();
					if (ItemHelper.sameItem(container, copy))
						continue;
					if (container.isEmpty())
						continue;

					// An ingredient is a flat set of items now, so it can only name the container's item.
					Ingredient bucket = Ingredient.of(stack.getItem());
					Identifier itemName = RegisteredObjectsHelper.getKeyOrThrow(stack.getItem());
					Identifier fluidName = RegisteredObjectsHelper.getKeyOrThrow(fluidCopy.getFluid());
					Identifier id = Create.asResource("fill_" + itemName.getNamespace() + "_" + itemName.getPath()
							+ "_with_" + fluidName.getNamespace() + "_" + fluidName.getPath());
					SizedFluidIngredient fluidIngredient = new SizedFluidIngredient(
						DataComponentFluidIngredient.of(false, fluidCopy), fluidCopy.getAmount());
					FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
							.withItemIngredients(bucket)
						.withFluidIngredients(fluidIngredient)
							.withSingleItemOutput(container)
							.build();
					consumer.accept(new RecipeHolder<>(recipeKey(id), recipe));
				}
			}
		}
	}

	/**
	 * A recipe is held under a registry key rather than a bare id in 26.2. These recipes only exist
	 * in JEI, so the key names something the recipe manager has never heard of.
	 */
	private static ResourceKey<Recipe<?>> recipeKey(Identifier id) {
		return ResourceKey.create(Registries.RECIPE, id);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, FillingRecipe recipe, IFocusGroup focuses) {
		builder
				.addSlot(RecipeIngredientRole.INPUT, 27, 51)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(recipe.getIngredients().get(0));

		addFluidSlot(builder, 27, 32, recipe.getRequiredFluid());

		builder
				.addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStack(getResultItem(recipe));
	}

	@Override
	public void draw(FillingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		spout.withFluids(fluidsOf(recipe.getRequiredFluid()))
			.draw(graphics, getWidth() / 2 - 13, 22);
	}

}
