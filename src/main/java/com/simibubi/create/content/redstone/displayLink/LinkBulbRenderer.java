package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LinkBulbRenderer
	extends SafeBlockEntityRenderer<LinkWithBulbBlockEntity, LinkBulbRenderer.LinkBulbRenderState> {

	public static class LinkBulbRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState tube;
		public @Nullable SuperByteBufferRenderState glow;
		public @Nullable Direction face;
	}

	public LinkBulbRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public LinkBulbRenderState createRenderState() {
		return new LinkBulbRenderState();
	}

	@Override
	protected void extractSafe(LinkWithBulbBlockEntity be, LinkBulbRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.tube = null;
		state.glow = null;

		float glow = be.getGlow(partialTicks);
		if (glow < .125f)
			return;

		glow = (float) (1 - (2 * Math.pow(glow - .75f, 2)));
		glow = Mth.clamp(glow, -1, 1);

		int color = (int) (200 * glow);

		BlockState blockState = be.getBlockState();
		state.face = be.getBulbFacing(blockState);

		var tube = CachedBufferer.partial(AllPartialModels.DISPLAY_LINK_TUBE, blockState);
		TransformStack.of(tube.getTransforms())
			.translate(be.getBulbOffset(blockState));
		state.tube = tube.light(LightCoordsUtil.FULL_BRIGHT)
			.extractRenderState();

		var bulb = CachedBufferer.partial(AllPartialModels.DISPLAY_LINK_GLOW, blockState);
		TransformStack.of(bulb.getTransforms())
			.translate(be.getBulbOffset(blockState));
		state.glow = bulb.light(LightCoordsUtil.FULL_BRIGHT)
			.color(color, color, color, 255)
			.disableDiffuse()
			.extractRenderState();
	}

	@Override
	protected void submitSafe(LinkBulbRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.tube == null || state.face == null)
			return;

		var msr = TransformStack.of(ms);
		ms.pushPose();

		msr.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(state.face) + 180)
			.rotateXDegrees(-AngleHelper.verticalAngle(state.face) - 90)
			.uncenter();

		state.tube.submit(ms, net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock(), queue);
		if (state.glow != null)
			state.glow.submit(ms, RenderTypes.additive(), queue);

		ms.popPose();
	}

}
