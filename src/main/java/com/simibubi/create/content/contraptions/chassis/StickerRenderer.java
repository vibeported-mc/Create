package com.simibubi.create.content.contraptions.chassis;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StickerRenderer extends SafeBlockEntityRenderer<StickerBlockEntity, StickerRenderer.StickerRenderState> {

	public static class StickerRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState head;
	}

	public StickerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public StickerRenderState createRenderState() {
		return new StickerRenderState();
	}

	@Override
	protected void extractSafe(StickerBlockEntity be, StickerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		SuperByteBuffer head = CachedBuffers.partial(AllPartialModels.STICKER_HEAD, blockState);
		float offset = be.piston.getValue(AnimationTickHolder.getPartialTicks(be.getLevel()));

		if (be.getLevel() != Minecraft.getInstance().level && !be.isVirtual())
			offset = blockState.getValue(StickerBlock.EXTENDED) ? 1 : 0;

		Direction facing = blockState.getValue(StickerBlock.FACING);
		TransformStack.of(head.getTransforms())
			.nudge(be.hashCode())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.uncenter()
			.translate(0, (offset * offset) * 4 / 16f, 0);

		state.head = head.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(StickerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.head != null)
			state.head.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

}
