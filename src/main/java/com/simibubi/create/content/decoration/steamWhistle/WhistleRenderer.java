package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WhistleRenderer extends SafeBlockEntityRenderer<WhistleBlockEntity, WhistleRenderer.WhistleRenderState> {

	public static class WhistleRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState mouth;
	}

	public WhistleRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public WhistleRenderState createRenderState() {
		return new WhistleRenderState();
	}

	@Override
	protected void extractSafe(WhistleBlockEntity be, WhistleRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.mouth = null;

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof WhistleBlock))
			return;

		Direction direction = blockState.getValue(WhistleBlock.FACING);
		WhistleSize size = blockState.getValue(WhistleBlock.SIZE);

		PartialModel mouth = size == WhistleSize.LARGE ? AllPartialModels.WHISTLE_MOUTH_LARGE
			: size == WhistleSize.MEDIUM ? AllPartialModels.WHISTLE_MOUTH_MEDIUM : AllPartialModels.WHISTLE_MOUTH_SMALL;

		float offset = be.animation.getValue(partialTicks);
		if (be.animation.getChaseTarget() > 0 && be.animation.getValue() > 0.5f) {
			float wiggleProgress = (AnimationTickHolder.getTicks(be.getLevel()) + partialTicks) / 8f;
			offset -= Math.sin(wiggleProgress * (2 * Mth.PI) * (4 - size.ordinal())) / 16f;
		}

		SuperByteBuffer buffer = CachedBufferer.partial(mouth, blockState);
		TransformStack.of(buffer.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(direction))
			.uncenter()
			.translate(0, offset * 4 / 16f, 0);
		state.mouth = buffer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(WhistleRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.mouth != null)
			state.mouth.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
