package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.fluids.FluidStack;

public class AnimatedSpout extends AnimatedKinetics {

	private List<FluidStack> fluids;

	public AnimatedSpout withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate(xOffset, yOffset);
		int scale = 20;

		blockElement(AllBlocks.SPOUT.getDefaultState())
			.scale(scale)
			.submit(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		matrixStack.pushMatrix();

		blockElement(AllPartialModels.SPOUT_TOP)
			.scale(scale)
			.submit(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f);
		blockElement(AllPartialModels.SPOUT_MIDDLE)
			.scale(scale)
			.submit(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f);
		blockElement(AllPartialModels.SPOUT_BOTTOM)
			.scale(scale)
			.submit(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f);

		matrixStack.popMatrix();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.submit(graphics);

		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		matrixStack.pushMatrix();
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		FluidStack fluidStack = fluids.get(0);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, from, from, to, to, to, graphics.bufferSource(), matrixStack, LightCoordsUtil.FULL_BRIGHT, false, true);
		matrixStack.popMatrix();

		float width = 1 / 128f * squeeze;
		matrixStack.translate(scale / 2f, scale * 1.5f);
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(16, 16);
		matrixStack.translate(-0.5f, 0);
		from = -width / 2 + 0.5f;
		to = width / 2 + 0.5f;
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, 0, from, to, 2, to, graphics.bufferSource(), matrixStack, LightCoordsUtil.FULL_BRIGHT, false, true);
		graphics.flush();
		Lighting.setupFor3DItems();

		matrixStack.popMatrix();
	}

}
