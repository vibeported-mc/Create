package com.simibubi.create.content.contraptions.actors.roller;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import java.util.List;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RollerRenderer extends SmartBlockEntityRenderer<RollerBlockEntity, RollerRenderer.RollerRenderState> {

	public static class RollerRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState wheel;
		public @Nullable SuperByteBufferRenderState frame;
	}

	public RollerRenderer(Context context) {
		super(context);
	}

	@Override
	public RollerRenderState createRenderState() {
		return new RollerRenderState();
	}

	@Override
	protected void extractSafe(RollerBlockEntity be, RollerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(RollerBlock.FACING);

		SuperByteBuffer wheel = CachedBufferer.partial(AllPartialModels.ROLLER_WHEEL, blockState);
		TransformStack.of(wheel.getTransforms())
			.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
				.scale(17 / 16f));
		HarvesterRenderer.transform(be.getLevel(), facing, wheel, be.getAnimatedSpeed(), Vec3.ZERO);
		TransformStack.of(wheel.getTransforms())
			.translate(0, -.5, .5)
			.rotateYDegrees(90);
		state.wheel = wheel.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer frame = CachedBufferer.partial(AllPartialModels.ROLLER_FRAME, blockState);
		TransformStack.of(frame.getTransforms())
			.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing) + 180), Direction.UP);
		state.frame = frame.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(RollerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		if (state.wheel != null) {
			ms.pushPose();
			ms.translate(0, -0.25, 0);
			state.wheel.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
			ms.popPose();
		}
		if (state.frame != null)
			state.frame.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		BlockState blockState = context.state;
		Direction facing = blockState.getValue(HORIZONTAL_FACING);
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.ROLLER_WHEEL, blockState);
		float speed = (float) (!VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: -context.getAnimationSpeed());
		if (context.contraption.stalled)
			speed = 0;

		superBuffer.transform(matrices.getModel());
		TransformStack.of(superBuffer.getTransforms())
			.translate(Vec3.atLowerCornerOf(facing.getUnitVec3i())
				.scale(17 / 16f));
		HarvesterRenderer.transform(context.world, facing, superBuffer, speed, Vec3.ZERO);

		PoseStack viewProjection = matrices.getViewProjection();
		viewProjection.pushPose();
		viewProjection.translate(0, -.25, 0);
		int contraptionWorldLight = LightCoordsUtil.getLightCoords(renderWorld, context.localPos);
		TransformStack.of(superBuffer.getTransforms())
			.translate(0, -.5, .5)
			.rotateYDegrees(90);
		superBuffer.light(contraptionWorldLight)
			.useLevelLight(renderWorld, matrices.getWorld());
		out.add(ActorGeometry.of(viewProjection, superBuffer, RenderTypes.cutoutMovingBlock()));
		viewProjection.popPose();

		SuperByteBuffer frame = CachedBufferer.partial(AllPartialModels.ROLLER_FRAME, blockState);
		frame.transform(matrices.getModel());
		TransformStack.of(frame.getTransforms())
			.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing) + 180), Direction.UP);
		frame.light(contraptionWorldLight)
			.useLevelLight(renderWorld, matrices.getWorld());
		out.add(ActorGeometry.of(viewProjection, frame, RenderTypes.cutoutMovingBlock()));
	}

}
