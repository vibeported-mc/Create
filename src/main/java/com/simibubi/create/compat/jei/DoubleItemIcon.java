package com.simibubi.create.compat.jei;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class DoubleItemIcon implements IDrawable {

	private Supplier<ItemStack> primarySupplier;
	private Supplier<ItemStack> secondarySupplier;
	private ItemStack primaryStack;
	private ItemStack secondaryStack;

	public DoubleItemIcon(Supplier<ItemStack> primary, Supplier<ItemStack> secondary) {
		this.primarySupplier = primary;
		this.secondarySupplier = secondary;
	}

	@Override
	public int getWidth() {
		return 18;
	}

	@Override
	public int getHeight() {
		return 18;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		Matrix3x2fStack matrixStack = graphics.pose();
		if (primaryStack == null) {
			primaryStack = primarySupplier.get();
			secondaryStack = secondarySupplier.get();
		}

		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));

		matrixStack.pushMatrix();
		matrixStack.translate((float) (1), (float) (1));
		GuiGameElement.of(primaryStack)
			.submit(graphics);
		matrixStack.popMatrix();

		matrixStack.pushMatrix();
		matrixStack.translate((float) (10), (float) (10));
		matrixStack.scale((float) (.5f), (float) (.5f));
		GuiGameElement.of(secondaryStack)
			.submit(graphics);
		matrixStack.popMatrix();

		matrixStack.popMatrix();
	}

}
