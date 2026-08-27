package com.simibubi.create.content.kinetics.speedController;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SpeedControllerRenderer
	extends SmartBlockEntityRenderer<SpeedControllerBlockEntity, SpeedControllerRenderer.SpeedControllerRenderState> {

	public static class SpeedControllerRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState shaft;
		public @Nullable SuperByteBufferRenderState bracket;
	}

	public SpeedControllerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public SpeedControllerRenderState createRenderState() {
		return new SpeedControllerRenderState();
	}

	@Override
	protected void extractSafe(SpeedControllerBlockEntity be, SpeedControllerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.shaft = null;
		state.bracket = null;

		if (!VisualizationManager.supportsVisualization(be.getLevel()))
			state.shaft = KineticBlockEntityRenderer
				.standardKineticRotationTransform(getRotatedModel(be), be, state.lightCoords)
				.extractRenderState();

		if (!be.hasBracket)
			return;

		BlockPos pos = be.getBlockPos();
		Level world = be.getLevel();
		BlockState blockState = be.getBlockState();
		boolean alongX = blockState.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Axis.X;

		SuperByteBuffer bracket = CachedBuffers.partial(AllPartialModels.SPEED_CONTROLLER_BRACKET, blockState);
		TransformStack.of(bracket.getTransforms())
			.translate(0, 1, 0)
			.rotateCentered((float) (alongX ? Math.PI : Math.PI / 2), Direction.UP);
		state.bracket = bracket.light(LightCoordsUtil.getLightCoords(world, pos.above()))
			.extractRenderState();
	}

	@Override
	protected void submitSafe(SpeedControllerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.shaft != null)
			state.shaft.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.bracket != null)
			state.bracket.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	private SuperByteBuffer getRotatedModel(SpeedControllerBlockEntity be) {
		return CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK,
			KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be)));
	}

}
