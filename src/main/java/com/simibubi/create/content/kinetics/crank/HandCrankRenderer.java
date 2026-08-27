package com.simibubi.create.content.kinetics.crank;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class HandCrankRenderer
	extends KineticBlockEntityRenderer<HandCrankBlockEntity, HandCrankRenderer.HandCrankRenderState> {

	public static class HandCrankRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState handle;
	}

	public HandCrankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public HandCrankRenderState createRenderState() {
		return new HandCrankRenderState();
	}

	@Override
	protected void extractSafe(HandCrankBlockEntity be, HandCrankRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		// Render states are reused between frames, so every field has to be assigned on every extract
		// or last frame's geometry lingers.
		if (be.shouldRenderShaft())
			super.extractSafe(be, state, partialTicks, cameraPosition);
		else
			state.model = null;

		Direction facing = be.getBlockState()
			.getValue(FACING);
		state.handle = kineticRotationTransform(be.getRenderedHandle(), be, facing.getAxis(),
			AngleHelper.rad(be.getIndependentAngle(partialTicks)), state.lightCoords).extractRenderState();
	}

	@Override
	protected void submitSafe(HandCrankRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.handle != null)
			state.handle.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
