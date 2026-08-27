package com.simibubi.create.foundation.blockEntity.renderer;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class ColoredOverlayBlockEntityRenderer<T extends BlockEntity, S extends ColoredOverlayBlockEntityRenderer.ColoredOverlayRenderState>
	extends SafeBlockEntityRenderer<T, S> {

	public static class ColoredOverlayRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState overlay;
	}

	public ColoredOverlayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new ColoredOverlayRenderState();
	}

	@Override
	protected void extractSafe(T be, S state, float partialTicks, Vec3 cameraPosition) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		state.overlay = render(getOverlayBuffer(be), getColor(be, partialTicks), state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.overlay != null)
			state.overlay.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	protected abstract int getColor(T be, float partialTicks);

	protected abstract SuperByteBuffer getOverlayBuffer(T be);

	public static SuperByteBuffer render(SuperByteBuffer buffer, int color, int light) {
		return buffer.color(color).light(light);
	}

}
