package com.simibubi.create.content.kinetics.flywheel;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FlywheelRenderer
	extends KineticBlockEntityRenderer<FlywheelBlockEntity, FlywheelRenderer.FlywheelRenderState> {

	public static class FlywheelRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState wheel;
	}

	public FlywheelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public FlywheelRenderState createRenderState() {
		return new FlywheelRenderState();
	}

	@Override
	protected void extractSafe(FlywheelBlockEntity be, FlywheelRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();

		float speed = be.visualSpeed.getValue(partialTicks) * 3 / 10f;
		float angle = be.angle + speed * partialTicks;

		SuperByteBuffer wheel = CachedBuffers.block(blockState);
		kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle), state.lightCoords);
		state.wheel = wheel.extractRenderState();
	}

	@Override
	protected void submitSafe(FlywheelRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.wheel != null)
			state.wheel.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected BlockState getRenderedBlockState(FlywheelBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
