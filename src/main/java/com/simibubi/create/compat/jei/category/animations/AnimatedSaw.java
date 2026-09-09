package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public class AnimatedSaw extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f + 90);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		matrixStack.translate((float) (0), (float) (0));
		matrixStack.translate((float) (2), (float) (22));
		int scale = 25;

		BlockState shaft = shaft(Direction.Axis.X);
		BlockState saw = AllBlocks.MECHANICAL_SAW.getDefaultState()
			.setValue(SawBlock.FACING, Direction.UP);
		float angle = getCurrentAngle();

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, modelOf(shaft), shaft, 0, 0, 0, -angle, 0, 0);
			part(ps, col, modelOf(saw), saw, 0, 0, 0, 0, 0, 0);
			part(ps, col, AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE.get(), null, 0, 0, 0, 0, -90, -90);
		});

		matrixStack.popMatrix();
	}

}
