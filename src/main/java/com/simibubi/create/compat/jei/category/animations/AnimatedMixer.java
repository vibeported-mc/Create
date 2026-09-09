package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class AnimatedMixer extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = 23;

		float animation = ((Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;
		float angle = getCurrentAngle();
		BlockState mixer = AllBlocks.MECHANICAL_MIXER.getDefaultState();
		BlockState basinState = AllBlocks.BASIN.getDefaultState();

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, cogwheel().get(), null, 0, 0, 0, 0, angle * 2, 0);
			part(ps, col, modelOf(mixer), mixer, 0, 0, 0, 0, 0, 0);
			part(ps, col, AllPartialModels.MECHANICAL_MIXER_POLE.get(), null, 0, animation, 0, 0, 0, 0);
			part(ps, col, AllPartialModels.MECHANICAL_MIXER_HEAD.get(), null, 0, animation, 0, 0, angle * 4, 0);
			part(ps, col, modelOf(basinState), basinState, 0, 1.65, 0, 0, 0, 0);
		});

		matrixStack.popMatrix();
	}

}
