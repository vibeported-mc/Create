package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EncasedFanRenderer
	extends KineticBlockEntityRenderer<EncasedFanBlockEntity, EncasedFanRenderer.EncasedFanRenderState> {

	public static class EncasedFanRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState shaftHalf;
		public @Nullable SuperByteBufferRenderState fanInner;
	}

	public EncasedFanRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EncasedFanRenderState createRenderState() {
		return new EncasedFanRenderState();
	}

	@Override
	protected void extractSafe(EncasedFanBlockEntity be, EncasedFanRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		// The fan draws its own shaft rather than the inherited kinetic model.
		state.model = null;
		state.shaftHalf = null;
		state.fanInner = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		Direction direction = be.getBlockState()
			.getValue(FACING);

		int lightBehind = LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos()
			.relative(direction.getOpposite()));
		int lightInFront = LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos()
			.relative(direction));

		SuperByteBuffer shaftHalf =
			CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());
		SuperByteBuffer fanInner =
			CachedBuffers.partialFacing(AllPartialModels.ENCASED_FAN_INNER, be.getBlockState(), direction.getOpposite());

		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float speed = be.getSpeed() * 5;
		if (speed > 0)
			speed = Mth.clamp(speed, 80, 64 * 20);
		if (speed < 0)
			speed = Mth.clamp(speed, -64 * 20, -80);
		float angle = (time * speed * 3 / 10f) % 360;
		angle = angle / 180f * (float) Math.PI;

		state.shaftHalf = standardKineticRotationTransform(shaftHalf, be, lightBehind).extractRenderState();
		state.fanInner = kineticRotationTransform(fanInner, be, direction.getAxis(), angle, lightInFront)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(EncasedFanRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.shaftHalf != null)
			state.shaftHalf.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		if (state.fanInner != null)
			state.fanInner.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

}
