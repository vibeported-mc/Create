package com.simibubi.create.compat.jei.category;

import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;

import java.util.Optional;

import com.simibubi.create.foundation.recipe.RecipeAccessors;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NullMarked;
import java.util.ArrayList;
import java.util.List;


import net.minecraft.world.item.Item;

import net.minecraft.world.item.crafting.ShapedRecipe;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.compat.jei.category.animations.AnimatedCrafter;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

@NullMarked
public class MechanicalCraftingCategory extends CreateRecipeCategory<CraftingRecipe> {

	private final AnimatedCrafter crafter = new AnimatedCrafter();

	public MechanicalCraftingCategory(Info<CraftingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CraftingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 81)
			.addItemStack(getResultItem(recipe));

		int x = getXPadding(recipe);
		int y = getYPadding(recipe);
		float scale = getScale(recipe);

		IIngredientRenderer<ItemStack> renderer = new CrafterIngredientRenderer(recipe);
		int i = 0;

		for (Optional<Ingredient> cell : grid(recipe)) {
			float f = 19 * scale;
			int xPosition = (int) (x + 1 + (i % getWidth(recipe)) * f);
			int yPosition = (int) (y + 1 + (i / getWidth(recipe)) * f);

			// A gap in the pattern still takes its place in the grid; it just holds no slot.
			cell.ifPresent(ingredient -> builder.addSlot(RecipeIngredientRole.INPUT, xPosition, yPosition)
				.setCustomRenderer(VanillaTypes.ITEM_STACK, renderer)
				.addIngredients(ingredient));

			i++;
		}

	}

	static int maxSize = 100;

	public static float getScale(CraftingRecipe recipe) {
		int w = getWidth(recipe);
		int h = getHeight(recipe);
		return Math.min(1, maxSize / (19f * Math.max(w, h)));
	}

	public static int getYPadding(CraftingRecipe recipe) {
		return 3 + 50 - (int) (getScale(recipe) * getHeight(recipe) * 19 * .5);
	}

	public static int getXPadding(CraftingRecipe recipe) {
		return 3 + 50 - (int) (getScale(recipe) * getWidth(recipe) * 19 * .5);
	}

	/**
	 * The recipe's grid, one entry per cell and empty where the pattern has a gap - which is what
	 * positions each ingredient, so the flat list {@link RecipeAccessors#ingredients} gives will not do.
	 *
	 * <p>1.21.1 had this from {@code getIngredients}, with an empty ingredient in each gap. 26.2 has
	 * no empty ingredient, and a mechanical crafting recipe is no longer a {@link ShapedRecipe}; both
	 * kinds still hand out their pattern's cells, as optionals.
	 */
	private static List<Optional<Ingredient>> grid(CraftingRecipe recipe) {
		if (recipe instanceof MechanicalCraftingRecipe mechanical)
			return mechanical.getIngredients();
		if (recipe instanceof ShapedRecipe shaped)
			return shaped.getIngredients();
		return RecipeAccessors.ingredients(recipe)
			.stream()
			.map(Optional::of)
			.toList();
	}

	private static int getWidth(CraftingRecipe recipe) {
		if (recipe instanceof MechanicalCraftingRecipe mechanical)
			return mechanical.getWidth();
		return recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 1;
	}

	private static int getHeight(CraftingRecipe recipe) {
		if (recipe instanceof MechanicalCraftingRecipe mechanical)
			return mechanical.getHeight();
		return recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 1;
	}

	@Override
	public void draw(CraftingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphicsExtractor graphics, double mouseX,
		double mouseY) {
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		float scale = getScale(recipe);
		List<Optional<Ingredient>> grid = grid(recipe);
		matrixStack.translate(getXPadding(recipe), getYPadding(recipe));

		for (int row = 0; row < getHeight(recipe); row++)
			for (int col = 0; col < getWidth(recipe); col++) {
				int pIndex = row * getWidth(recipe) + col;
				if (pIndex >= grid.size())
					break;
				if (grid.get(pIndex)
					.isEmpty())
					continue;
				matrixStack.pushMatrix();
				matrixStack.translate((float) (col * 19 * scale), (float) (row * 19 * scale));
				matrixStack.scale((float) (scale), (float) (scale));
				AllGuiTextures.JEI_SLOT.render(graphics, 0, 0);
				matrixStack.popMatrix();
			}

		matrixStack.popMatrix();

		AllGuiTextures.JEI_SLOT.render(graphics, 133, 80);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 128, 59);
		crafter.draw(graphics, 129, 25);

		matrixStack.pushMatrix();
		matrixStack.translate((float) (0), (float) (0));

		// One crafter per filled cell.
		long amount = grid.stream()
			.filter(Optional::isPresent)
			.count();

		graphics.text(Minecraft.getInstance().font, amount + "", 142, 39, 0xFFFFFFFF);
		matrixStack.popMatrix();
	}

	private static final class CrafterIngredientRenderer implements IIngredientRenderer<ItemStack> {

		private final CraftingRecipe recipe;
		private final float scale;

		public CrafterIngredientRenderer(CraftingRecipe recipe) {
			this.recipe = recipe;
			scale = getScale(recipe);
		}

		@Override
		public void render(GuiGraphicsExtractor graphics, @NotNull ItemStack ingredient) {
			Matrix3x2fStack matrixStack = graphics.pose();
			matrixStack.pushMatrix();
			float scale = getScale(recipe);
			matrixStack.scale((float) (scale), (float) (scale));

			// The model-view stack no longer feeds GUI drawing - the 2D pose stack above is what
			// items are placed by - so the push/apply/pop around this is gone.
			if (ingredient != null) {
				Minecraft minecraft = Minecraft.getInstance();
				Font font = getFontRenderer(minecraft, ingredient);
				graphics.item(ingredient, 0, 0);
				graphics.itemDecorations(font, ingredient, 0, 0, null);
			}

			matrixStack.popMatrix();
		}

		@Override
		public int getWidth() {
			return (int) (16 * scale);
		}

		@Override
		public int getHeight() {
			return (int) (16 * scale);
		}

		@Override
		public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			try {
				return ingredient.getTooltipLines(Item.TooltipContext.of(minecraft.level), player, tooltipFlag);
			} catch (RuntimeException | LinkageError e) {
				List<Component> list = new ArrayList<>();
                MutableComponent crash = Component.translatable("jei.tooltip.error.crash");
				list.add(crash.withStyle(ChatFormatting.RED));
				return list;
			}
		}
	}

}
