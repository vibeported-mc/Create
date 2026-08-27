package com.simibubi.create.content.logistics.funnel;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FunnelRenderer
	extends SmartBlockEntityRenderer<FunnelBlockEntity, FunnelRenderer.FunnelRenderState> {

	public static class FunnelRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState flap;
		public @Nullable Direction funnelFacing;
		public float flapness;
		public float flapOffset;
	}

	public FunnelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public FunnelRenderState createRenderState() {
		return new FunnelRenderState();
	}

	@Override
	protected void extractSafe(FunnelBlockEntity be, FunnelRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.flap = null;

		if (!be.hasFlap() || VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		PartialModel partialModel = (blockState.getBlock() instanceof FunnelBlock ? AllPartialModels.FUNNEL_FLAP
			: AllPartialModels.BELT_FUNNEL_FLAP);
		SuperByteBuffer flapBuffer = CachedBuffers.partial(partialModel, blockState);

		state.funnelFacing = FunnelBlock.getFunnelFacing(blockState);
		state.flapness = be.flap.getValue(partialTicks);
		state.flapOffset = -be.getFlapOffset();
		state.flap = flapBuffer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(FunnelRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.flap != null && state.funnelFacing != null)
			FlapStuffs.submitFlaps(ms, queue, state.flap, FlapStuffs.FUNNEL_PIVOT, state.funnelFacing, state.flapness,
				state.flapOffset);
	}

}
