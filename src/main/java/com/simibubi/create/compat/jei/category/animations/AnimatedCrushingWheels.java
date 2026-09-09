package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AnimatedCrushingWheels extends AnimatedKinetics {

	private final BlockState wheel = AllBlocks.CRUSHING_WHEEL.getDefaultState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X);

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(0, -22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = 22;

		float angle = getCurrentAngle();

		scene(graphics, scale, GuiElementTransform.WIDE_ROOM, (ps, col) -> {
			part(ps, col, modelOf(wheel), wheel, 0, 0, 0, 0, 90, -angle);
			part(ps, col, modelOf(wheel), wheel, 2, 0, 0, 0, 90, angle);
		});

		matrixStack.popMatrix();
	}

}
