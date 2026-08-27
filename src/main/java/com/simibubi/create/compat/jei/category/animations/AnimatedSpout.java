package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
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
		matrixStack.translate((float) (xOffset), (float) (yOffset));
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
		matrixStack.translate((float) (0), (float) (-3 * squeeze / 32f));
		blockElement(AllPartialModels.SPOUT_MIDDLE)
			.scale(scale)
			.submit(graphics);
		matrixStack.translate((float) (0), (float) (-3 * squeeze / 32f));
		blockElement(AllPartialModels.SPOUT_BOTTOM)
			.scale(scale)
			.submit(graphics);
		matrixStack.translate((float) (0), (float) (-3 * squeeze / 32f));

		matrixStack.popMatrix();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.submit(graphics);

		AnimatedKinetics.DEFAULT_LIGHTING.apply();

		// Both fluid boxes used to ride the same pose stack as the blocks above, flipped and scaled
		// to block units by hand. A picture-in-picture pass does that part itself, so what is left to
		// hand over is the pixels-per-block the boxes were drawn at and, for the falling stream, the
		// offset it used to be translated by - converted from pixels into blocks on the same axes.
		FluidStack fluidStack = fluids.get(0);
		float from = 3f / 16f;
		float to = 17f / 16f;
		sceneGeometry(graphics, 16, 0, 0, 0, (poseStack, queue) -> FluidRenderHelper.submitFluidBox(queue, fluidStack,
			from, from, from, to, to, to, poseStack, LightCoordsUtil.FULL_BRIGHT, false, true));

		float width = 1 / 128f * squeeze;
		float streamFrom = -width / 2 + 0.5f;
		float streamTo = width / 2 + 0.5f;
		sceneGeometry(graphics, 16, scale / 2f / 16 - 0.5f, scale * -1.5f / 16, scale / 2f / 16 - 0.5f,
			(poseStack, queue) -> FluidRenderHelper.submitFluidBox(queue, fluidStack, streamFrom, 0, streamFrom,
				streamTo, 2, streamTo, poseStack, LightCoordsUtil.FULL_BRIGHT, false, true));

		ILightingSettings.ITEMS_3D.apply();

		matrixStack.popMatrix();
	}

}
