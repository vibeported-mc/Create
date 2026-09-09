package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;

import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
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

		BlockState drain = AllBlocks.ITEM_DRAIN.getDefaultState();
		float from = 2 / 16f;
		float to = 1f - from;

		// The fluid goes in the same pass as the drain so that the rim occludes it on depth, the way
		// it did when both rode one pose stack.
		scene(graphics, scale, (ps, col) -> {
			part(ps, col, modelOf(drain), drain, 0, 0, 0, 0, 0, 0);

			ps.pushPose();
			GuiElementTransform.flipForGuiRender(ps);
			FluidRenderHelper.submitFluidBox(col, fluid, from, from, from, to, 3 / 4f, to, ps,
				LightCoordsUtil.FULL_BRIGHT, false, true);
			ps.popPose();
		});

		matrixStack.popMatrix();
	}
}
