package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class AnimatedCrafter extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-12.5f, -22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);

		matrixStack.translate((float) (3), (float) (16));
		int scale = 22;

		BlockState crafter = AllBlocks.MECHANICAL_CRAFTER.getDefaultState();
		float angle = getCurrentAngle();

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, cogwheel().get(), null, 0, 0, 0, 90, 0, angle);
			part(ps, col, modelOf(crafter), crafter, 0, 0, 0, 0, 180, 0);
		});

		matrixStack.popMatrix();
	}

}
