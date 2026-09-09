package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public class AnimatedPress extends AnimatedKinetics {

	private boolean basin;

	public AnimatedPress(boolean basin) {
		this.basin = basin;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = basin ? 23 : 24;

		BlockState shaft = shaft(Direction.Axis.Z);
		BlockState press = AllBlocks.MECHANICAL_PRESS.getDefaultState();
		BlockState basinState = AllBlocks.BASIN.getDefaultState();
		float angle = getCurrentAngle();
		float headOffset = getAnimatedHeadOffset();

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, modelOf(shaft), shaft, 0, 0, 0, 0, 0, angle);
			part(ps, col, modelOf(press), press, 0, 0, 0, 0, 0, 0);
			part(ps, col, AllPartialModels.MECHANICAL_PRESS_HEAD.get(), null, 0, -headOffset, 0, 0, 0, 0);
			if (basin)
				part(ps, col, modelOf(basinState), basinState, 0, 1.65, 0, 0, 0, 0);
		});

		matrixStack.popMatrix();
	}

	private float getAnimatedHeadOffset() {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		if (cycle < 10) {
			float progress = cycle / 10;
			return -(progress * progress * progress);
		}
		if (cycle < 15)
			return -1;
		if (cycle < 20)
			return -1 + (1 - ((20 - cycle) / 5));
		return 0;
	}

}
