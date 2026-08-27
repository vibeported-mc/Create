package com.simibubi.create.content.kinetics.drill;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class DrillRenderer extends KineticBlockEntityRenderer<DrillBlockEntity> {

	public DrillRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(DrillBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacing(AllPartialModels.DRILL_HEAD, state);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, SubmitNodeCollector buffer) {
		BlockState state = context.state;
		SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.DRILL_HEAD, state);
		Direction facing = state.getValue(DrillBlock.FACING);

		float speed = (float) (context.contraption.stalled
				|| !VecHelper.isVecPointingTowards(context.relativeMotion, facing
				.getOpposite()) ? context.getAnimationSpeed() : 0);
		float time = AnimationTickHolder.getRenderTime() / 20;
		float angle = (float) (((time * speed) % 360));

		superBuffer
			.transform(matrices.getModel())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing))
			.rotateZDegrees(angle)
			.uncenter()
			.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld())
			.submit(matrices.getViewProjection(), RenderTypes.solidMovingBlock(), buffer);
	}

}
