package com.simibubi.create.content.kinetics.mixer;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MechanicalMixerRenderer
	extends KineticBlockEntityRenderer<MechanicalMixerBlockEntity, MechanicalMixerRenderer.MixerRenderState> {

	public static class MixerRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState cogwheel;
		public @Nullable SuperByteBufferRenderState pole;
		public @Nullable SuperByteBufferRenderState head;
	}

	public MechanicalMixerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public MixerRenderState createRenderState() {
		return new MixerRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected void extractSafe(MechanicalMixerBlockEntity be, MixerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		// The mixer draws its own cogwheel rather than the inherited kinetic model.
		state.model = null;
		state.cogwheel = null;
		state.pole = null;
		state.head = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();

		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		state.cogwheel = standardKineticRotationTransform(superBuffer, be, state.lightCoords).extractRenderState();

		float renderedHeadOffset = be.getRenderedHeadOffset(partialTicks);
		float speed = be.getRenderedHeadRotationSpeed(partialTicks);
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float angle = ((time * speed * 6 / 10f) % 360) / 180 * (float) Math.PI;

		SuperByteBuffer poleRender = CachedBufferer.partial(AllPartialModels.MECHANICAL_MIXER_POLE, blockState);
		TransformStack.of(poleRender.getTransforms())
			.translate(0, -renderedHeadOffset, 0);
		state.pole = poleRender.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer headRender = CachedBufferer.partial(AllPartialModels.MECHANICAL_MIXER_HEAD, blockState);
		TransformStack.of(headRender.getTransforms())
			.rotateCentered(angle, Direction.UP)
			.translate(0, -renderedHeadOffset, 0);
		state.head = headRender.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(MixerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.cogwheel != null)
			state.cogwheel.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.pole != null)
			state.pole.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.head != null)
			state.head.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

}
