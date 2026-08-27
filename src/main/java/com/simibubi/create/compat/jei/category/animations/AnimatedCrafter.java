package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class AnimatedCrafter extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate(xOffset, yOffset);
		AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);

		matrixStack.translate(3, 16);
		TransformStack.of(matrixStack)
			.rotateXDegrees(-12.5f)
			.rotateYDegrees(-22.5f);
		int scale = 22;

		blockElement(cogwheel())
			.rotateBlock(90, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MECHANICAL_CRAFTER.getDefaultState())
			.rotateBlock(0, 180, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.popMatrix();
	}

}
