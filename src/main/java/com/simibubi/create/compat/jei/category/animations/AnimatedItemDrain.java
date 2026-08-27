package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.LightCoordsUtil;

import net.neoforged.neoforge.fluids.FluidStack;

public class AnimatedItemDrain extends AnimatedKinetics {

	private FluidStack fluid;

	public AnimatedItemDrain withFluid(FluidStack fluid) {
		this.fluid = fluid;
		return this;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = 20;

		blockElement(AllBlocks.ITEM_DRAIN.getDefaultState())
			.scale(scale)
			.submit(graphics);

		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale((float) (scale), (float) (scale));
		float from = 2 / 16f;
		float to = 1f - from;
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, from, from, from, to, 3 / 4f, to, graphics.bufferSource(), matrixStack, LightCoordsUtil.FULL_BRIGHT, false, true);
		graphics.flush();

		matrixStack.popMatrix();
	}
}
