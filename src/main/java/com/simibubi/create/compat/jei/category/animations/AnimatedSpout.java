package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
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

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		FluidStack fluidStack = fluids.get(0);
		float from = 3f / 16f;
		float to = 17f / 16f;
		float width = 1 / 128f * squeeze;
		float streamFrom = -width / 2 + 0.5f;
		float streamTo = width / 2 + 0.5f;

		BlockState spout = AllBlocks.SPOUT.getDefaultState();
		BlockState depot = AllBlocks.DEPOT.getDefaultState();
		// Each segment was nudged along the pose stack in pixels between draws; a scene works in
		// blocks, so the same nudge is that many of the widget's pixels to the block.
		float nudge = -3 * squeeze / 32f / scale;

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, modelOf(spout), spout, 0, 0, 0, 0, 0, 0);

			ps.pushPose();
			part(ps, col, AllPartialModels.SPOUT_TOP.get(), null, 0, 0, 0, 0, 0, 0);
			ps.translate(0, nudge, 0);
			part(ps, col, AllPartialModels.SPOUT_MIDDLE.get(), null, 0, 0, 0, 0, 0, 0);
			ps.translate(0, nudge, 0);
			part(ps, col, AllPartialModels.SPOUT_BOTTOM.get(), null, 0, 0, 0, 0, 0, 0);
			ps.popPose();

			part(ps, col, modelOf(depot), depot, 0, 2, 0, 0, 0, 0);

			// The fluid belongs in the same pass as the spout, or it is composited over it instead of
			// being occluded by it. Both boxes were drawn at sixteen pixels to the unit while the
			// blocks around them go at the widget's own scale, so they keep that ratio here.
			float unitsToBlocks = 16f / scale;

			ps.pushPose();
			GuiElementTransform.flipForGuiRender(ps);
			ps.scale(unitsToBlocks, unitsToBlocks, unitsToBlocks);
			FluidRenderHelper.submitFluidBox(col, fluidStack, from, from, from, to, to, to, ps,
				LightCoordsUtil.FULL_BRIGHT, false, true);
			ps.popPose();

			ps.pushPose();
			ps.translate(0.5F, 1.5F, 0.5F);
			GuiElementTransform.flipForGuiRender(ps);
			ps.scale(unitsToBlocks, unitsToBlocks, unitsToBlocks);
			ps.translate(-0.5F, 0, -0.5F);
			FluidRenderHelper.submitFluidBox(col, fluidStack, streamFrom, 0, streamFrom, streamTo, 2, streamTo, ps,
				LightCoordsUtil.FULL_BRIGHT, false, true);
			ps.popPose();
		});

		matrixStack.popMatrix();
	}

}
