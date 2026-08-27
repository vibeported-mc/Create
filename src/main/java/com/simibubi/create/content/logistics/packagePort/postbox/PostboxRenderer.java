package com.simibubi.create.content.logistics.packagePort.postbox;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.Transform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PostboxRenderer extends SmartBlockEntityRenderer<PostboxBlockEntity, PostboxRenderer.PostboxRenderState> {

	public static class PostboxRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState flag;
	}

	public PostboxRenderer(Context context) {
		super(context);
	}

	@Override
	public PostboxRenderState createRenderState() {
		return new PostboxRenderState();
	}

	@Override
	protected void extractSafe(PostboxBlockEntity be, PostboxRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);

		state.nameplate = null;
		if (be.addressFilter != null && !be.addressFilter.isBlank())
			state.nameplate = extractNameplateOnHover(be, Component.literal(be.addressFilter), 1, cameraPosition,
				state.lightCoords);

		SuperByteBuffer sbb = CachedBufferer.partial(AllPartialModels.POSTBOX_FLAG, be.getBlockState());

		var msr = TransformStack.of(sbb.getTransforms());
		msr.rotateCentered(Mth.DEG_TO_RAD * (180 - be.getBlockState()
			.getValue(PostboxBlock.FACING)
			.toYRot()), Axis.YP);

		transformFlag(msr, be, partialTicks);

		state.flag = sbb.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(PostboxRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.flag != null)
			state.flag.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	public static void transformFlag(Transform<?> flag, PostboxBlockEntity be, float partialTicks) {
		float value = be.flag.getValue(partialTicks);
		float progress = (float) (Math.pow(Math.min(value * 5, 1), 2));
		if (be.flag.getChaseTarget() > 0 && !be.flag.settled() && progress == 1) {
			float wiggleProgress = (value - .2f) / .8f;
			progress += (Math.sin(wiggleProgress * (2 * Mth.PI) * 4) / 8f) / Math.max(1, 8f * wiggleProgress);
		}
		flag.translate(0, 10 / 16f, 2 / 16f);
		flag.rotateXDegrees(-progress * 90);
		flag.translateBack(0, 10 / 16f, 2 / 16f);
	}

}
