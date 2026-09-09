package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.api.client.gui.element.GuiElementGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.state.BlockState;

public class AnimatedMillstone extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);
		matrixStack.translate((float) (-2), (float) (18));
		int scale = 22;

		BlockState millstone = AllBlocks.MILLSTONE.getDefaultState();
		var millstoneModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(millstone);
		var cogModel = AllPartialModels.MILLSTONE_COG.get();
		float cogAngle = getCurrentAngle() * 2;

		// One pass for both parts. Drawn as two elements they would be composited as separate flat
		// quads and the body would hide the cog outright, whichever order they went in; sharing a
		// pass gives them the one depth buffer they had on 1.21.1, where the cog shows through the
		// hole in the top because it is nearer than the floor of it.
		sceneGeometry(graphics, scale, 0, 0, 0, (poseStack, collector) -> {
			poseStack.pushPose();
			GuiElementGeometry.rotateBlock(poseStack, 22.5, cogAngle, 0);
			GuiElementGeometry.submitBlockModel(poseStack, collector, cogModel, null, null, 0xFFFFFFFF);
			poseStack.popPose();

			poseStack.pushPose();
			GuiElementGeometry.rotateBlock(poseStack, 22.5, 22.5, 0);
			GuiElementGeometry.submitBlockModel(poseStack, collector, millstoneModel, millstone, null, 0xFFFFFFFF);
			poseStack.popPose();
		});

		matrixStack.popMatrix();
	}

}
