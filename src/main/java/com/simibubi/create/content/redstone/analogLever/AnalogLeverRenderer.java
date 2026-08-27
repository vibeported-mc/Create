package com.simibubi.create.content.redstone.analogLever;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class AnalogLeverRenderer
	extends SafeBlockEntityRenderer<AnalogLeverBlockEntity, AnalogLeverRenderer.AnalogLeverRenderState> {

	public static class AnalogLeverRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState handle;
		public @Nullable SuperByteBufferRenderState indicator;
	}

	public AnalogLeverRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public AnalogLeverRenderState createRenderState() {
		return new AnalogLeverRenderState();
	}

	@Override
	protected void extractSafe(AnalogLeverBlockEntity be, AnalogLeverRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.handle = null;
		state.indicator = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState leverState = be.getBlockState();
		float value = be.clientState.getValue(partialTicks);

		// Handle
		SuperByteBuffer handle = CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_HANDLE, leverState);
		float angle = (float) ((value / 15) * 90 / 180 * Math.PI);
		TransformStack.of(transform(handle, leverState).getTransforms())
			.translate(1 / 2f, 1 / 16f, 1 / 2f)
			.rotate(angle, Direction.EAST)
			.translate(-1 / 2f, -1 / 16f, -1 / 2f);
		state.handle = handle.light(state.lightCoords)
			.extractRenderState();

		// Indicator
		int color = Color.mixColors(0x2C0300, 0xCD0000, value / 15f);
		SuperByteBuffer indicator =
			transform(CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, leverState), leverState);
		state.indicator = indicator.light(state.lightCoords)
			.color(color)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(AnalogLeverRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.handle != null)
			state.handle.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.indicator != null)
			state.indicator.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState leverState) {
		AttachFace face = leverState.getValue(AnalogLeverBlock.FACE);
		float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
		float rY = AngleHelper.horizontalAngle(leverState.getValue(AnalogLeverBlock.FACING));
		TransformStack.of(buffer.getTransforms())
			.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP)
			.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
		return buffer;
	}

}
