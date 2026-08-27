package com.simibubi.create.compat.jei.category;

import com.simibubi.create.foundation.recipe.RecipeAccessors;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NullMarked;
import java.util.List;
import java.util.function.Supplier;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

@NullMarked
public abstract class ProcessingViaFanCategory<T extends Recipe<?>> extends CreateRecipeCategory<T> {

	protected static final int SCALE = 24;

	public ProcessingViaFanCategory(Info<T> info) {
		super(info);
	}

	public static Supplier<ItemStack> getFan(String name) {
		ItemStack stack = AllBlocks.ENCASED_FAN.asStack();
		stack.set(DataComponents.CUSTOM_NAME, CreateLang.translateDirect("recipe." + name + ".fan").withStyle(style -> style.withItalic(false)));
		return () -> stack;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
		builder
				.addSlot(RecipeIngredientRole.INPUT, 21, 48)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(RecipeAccessors.ingredients(recipe).get(0));
		builder
				.addSlot(RecipeIngredientRole.OUTPUT, 141, 48)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStack(getResultItem(recipe));
	}

	@Override
	public void draw(T recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		renderWidgets(graphics, recipe, mouseX, mouseY);

		Matrix3x2fStack matrixStack = graphics.pose();

		matrixStack.pushMatrix();
		translateFan(matrixStack);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-12.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		AnimatedKinetics.defaultBlockElement(AllPartialModels.ENCASED_FAN_INNER)
			.rotateBlock(180, 0, AnimatedKinetics.getCurrentAngle() * 16)
			.scale(SCALE)
			.submit(graphics);

		AnimatedKinetics.defaultBlockElement(AllBlocks.ENCASED_FAN.getDefaultState())
			.rotateBlock(0, 180, 0)
			.atLocal(0, 0, 0)
			.scale(SCALE)
			.submit(graphics);

		renderAttachedBlock(graphics);
		matrixStack.popMatrix();
	}

	protected void renderWidgets(GuiGraphicsExtractor graphics, T recipe, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
		getBlockShadow().render(graphics, 65, 39);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 54, 51);
	}

	protected AllGuiTextures getBlockShadow() {
		return AllGuiTextures.JEI_SHADOW;
	}

	protected void translateFan(PoseStack matrixStack) {
		matrixStack.translate((float) (56), (float) (33));
	}

	protected abstract void renderAttachedBlock(GuiGraphicsExtractor graphics);

	public static abstract class MultiOutput<T extends StandardProcessingRecipe<?>> extends ProcessingViaFanCategory<T> {

		public MultiOutput(Info<T> info) {
			super(info);
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
			List<ProcessingOutput> results = recipe.getRollableResults();
			int xOffsetAmount = 1 - Math.min(3, results.size());

			builder
					.addSlot(RecipeIngredientRole.INPUT, 5 * xOffsetAmount + 21, 48)
					.setBackground(getRenderedSlot(), -1, -1)
					.addIngredients(recipe.getIngredients().get(0));

			int i = 0;
			boolean excessive = results.size() > 9;
			for (ProcessingOutput output : results) {
				int xOffset = (i % 3) * 19 + 9 * xOffsetAmount;
				int yOffset = (i / 3) * -19 + (excessive ? 8 : 0);

				builder
						.addSlot(RecipeIngredientRole.OUTPUT, 141 + xOffset, 48 + yOffset)
						.setBackground(getRenderedSlot(output), -1, -1)
						.addItemStack(output.getStack())
						.addRichTooltipCallback(addStochasticTooltip(output));
				i++;
			}
		}

		@Override
		protected void renderWidgets(GuiGraphicsExtractor graphics, T recipe, double mouseX, double mouseY) {
			int size = recipe.getRollableResultsAsItemStacks().size();
			int xOffsetAmount = 1 - Math.min(3, size);

			AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
			getBlockShadow().render(graphics, 65, 39);
			AllGuiTextures.JEI_LONG_ARROW.render(graphics, 7 * xOffsetAmount + 54, 51);

		}

	}

}
