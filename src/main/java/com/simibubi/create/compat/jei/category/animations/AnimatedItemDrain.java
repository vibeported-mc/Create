package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;

import net.createmod.catnip.api.client.render.FluidRenderHelper;
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

		// The flip and the scale to block units are what a picture-in-picture pass already does for
		// the geometry it draws, so only the widget's pixels-per-block is left to hand over.
		float from = 2 / 16f;
		float to = 1f - from;
		sceneGeometry(graphics, scale, 0, 0, 0, (poseStack, queue) -> FluidRenderHelper.submitFluidBox(queue, fluid,
			from, from, from, to, 3 / 4f, to, poseStack, LightCoordsUtil.FULL_BRIGHT, false, true));

		matrixStack.popMatrix();
	}
}
