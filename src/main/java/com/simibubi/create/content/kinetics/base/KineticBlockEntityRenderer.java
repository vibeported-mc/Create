package com.simibubi.create.content.kinetics.base;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class KineticBlockEntityRenderer<T extends KineticBlockEntity, S extends KineticBlockEntityRenderer.KineticRenderState>
	extends SafeBlockEntityRenderer<T, S> {

	public static final SuperByteBufferCache.Compartment<BlockState> KINETIC_BLOCK = new SuperByteBufferCache.Compartment<>();
	public static boolean rainbowMode = false;

	public static class KineticRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState model;
		public RenderType renderType = RenderTypes.cutoutMovingBlock();
	}

	public KineticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new KineticRenderState();
	}

	@Override
	protected void extractSafe(T be, S state, float partialTicks, Vec3 cameraPosition) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState renderedState = getRenderedBlockState(be);
		state.renderType = getRenderType(be, renderedState);
		state.model = standardKineticRotationTransform(getRotatedModel(be, renderedState), be, state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.model != null)
			state.model.submit(ms, state.renderType, queue);
	}

	protected BlockState getRenderedBlockState(T be) {
		return be.getBlockState();
	}

	/**
	 * Minecraft 26.2 derives a block's chunk layer from its texture's alpha channel rather than
	 * exposing it on the model, so the old scan over the model's render types has nothing to read.
	 * Cutout is the safe default - it draws opaque geometry correctly, just with an alpha test that
	 * always passes - and matches what the old scan fell back to. Override for translucent blocks.
	 */
	protected RenderType getRenderType(T be, BlockState state) {
		return RenderTypes.cutoutMovingBlock();
	}

	protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
		return CachedBuffers.block(KINETIC_BLOCK, state);
	}

	public static SuperByteBufferRenderState extractRotatingKineticBlock(KineticBlockEntity be,
		BlockState renderedState, int light) {
		SuperByteBuffer superByteBuffer = CachedBuffers.block(KINETIC_BLOCK, renderedState);
		return standardKineticRotationTransform(superByteBuffer, be, light).extractRenderState();
	}

	public static float getAngleForBe(KineticBlockEntity be, final BlockPos pos, Axis axis) {
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float offset = getRotationOffsetForPosition(be, pos, axis);
		float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
		return angle;
	}

	public static SuperByteBuffer standardKineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be,
		int light) {
		final BlockPos pos = be.getBlockPos();
		Axis axis = ((IRotate) be.getBlockState()
			.getBlock()).getRotationAxis(be.getBlockState());
		return kineticRotationTransform(buffer, be, axis, getAngleForBe(be, pos, axis), light);
	}

	public static SuperByteBuffer kineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be, Axis axis,
		float angle, int light) {
		buffer.light(light);
		// Catnip's 26.x SuperByteBuffer is a minimal interface and no longer implements Flywheel's
		// transform API itself; transforms go through the PoseStack it exposes.
		TransformStack.of(buffer.getTransforms())
			.rotateCentered(angle, axis);

		if (KineticDebugger.isActive()) {
			rainbowMode = true;
			buffer.color(be.hasNetwork() ? Color.generateFromLong(be.network) : Color.WHITE);
		} else {
			float overStressedEffect = be.effects.overStressedEffect;
			if (overStressedEffect != 0) {
				boolean overstressed = overStressedEffect > 0;
				Color color = overstressed ? Color.RED : Color.SPRING_GREEN;
				float weight = overstressed ? overStressedEffect : -overStressedEffect;

				buffer.color(Color.WHITE.mixWith(color, weight));
			} else {
				buffer.color(Color.WHITE);
			}
		}

		return buffer;
	}

	public static float getRotationOffsetForPosition(KineticBlockEntity be, final BlockPos pos, final Axis axis) {
		return KineticBlockEntityVisual.rotationOffset(be.getBlockState(), axis, pos) + be.getRotationAngleOffset(axis);
	}

	public static BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState()
			.setValue(BlockStateProperties.AXIS, axis);
	}

	public static Axis getRotationAxisOf(KineticBlockEntity be) {
		return ((IRotate) be.getBlockState()
			.getBlock()).getRotationAxis(be.getBlockState());
	}

}
