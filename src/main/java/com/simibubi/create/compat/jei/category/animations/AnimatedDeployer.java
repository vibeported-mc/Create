package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public class AnimatedDeployer extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = 20;

		BlockState shaft = shaft(Direction.Axis.Z);
		BlockState deployer = AllBlocks.DEPLOYER.getDefaultState()
			.setValue(DeployerBlock.FACING, Direction.DOWN)
			.setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false);
		BlockState depot = AllBlocks.DEPOT.getDefaultState();
		float angle = getCurrentAngle();

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float reach = cycle < 10 ? cycle / 10f : cycle < 20 ? (20 - cycle) / 10f : 0;
		// Seventeen pixels on 1.21.1, where this was pushed onto the pose stack before the element
		// scaled itself. A scene works in blocks, so it is seventeen of the widget's pixels per block.
		float reachInBlocks = reach * 17 / scale;

		scene(graphics, scale, TALL_ROOM, (ps, col) -> {
			part(ps, col, modelOf(shaft), shaft, 0, 0, 0, 0, 0, angle);
			part(ps, col, modelOf(deployer), deployer, 0, 0, 0, 0, 0, 0);

			ps.pushPose();
			ps.translate(0, reachInBlocks, 0);
			part(ps, col, AllPartialModels.DEPLOYER_POLE.get(), null, 0, 0, 0, 90, 0, 0);
			part(ps, col, AllPartialModels.DEPLOYER_HAND_HOLDING.get(), null, 0, 0, 0, 90, 0, 0);
			ps.popPose();

			part(ps, col, modelOf(depot), depot, 0, 2, 0, 0, 0, 0);
		});

		matrixStack.popMatrix();
	}

}
