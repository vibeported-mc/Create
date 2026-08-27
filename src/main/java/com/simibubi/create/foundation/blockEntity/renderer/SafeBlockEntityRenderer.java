package com.simibubi.create.foundation.blockEntity.renderer;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.simibubi.create.foundation.mixin.accessor.LevelRendererAccessor;

import net.createmod.ponder.api.client.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Base for Create's block entity renderers.
 * <p>
 * Minecraft 26.2 splits rendering into an extract phase, which runs on the client thread and copies
 * what the renderer needs off the block entity, and a submit phase, which queues geometry from that
 * copy and may run on another thread. The old {@code renderSafe} hook is therefore split into
 * {@link #extractSafe} and {@link #submitSafe}: read the block entity in the first, and touch nothing
 * but the render state in the second.
 */
public abstract class SafeBlockEntityRenderer<T extends BlockEntity, S extends SafeBlockEntityRenderer.SafeRenderState>
	implements BlockEntityRenderer<T, S> {

	public static class SafeRenderState extends BlockEntityRenderState {
		/**
		 * Set during extraction when there is nothing to draw. Submission is still called for a state
		 * that was never extracted, so the flag has to be carried rather than simply skipping work.
		 */
		public boolean skip;
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new SafeRenderState();
	}

	@Override
	public final void extractRenderState(T be, S state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		state.skip = isInvalid(be);
		if (state.skip)
			return;
		BlockEntityRenderState.extractBase(be, state, breakProgress);
		extractSafe(be, state, partialTicks, cameraPosition);
	}

	protected abstract void extractSafe(T be, S state, float partialTicks, Vec3 cameraPosition);

	@Override
	public final void submit(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.skip)
			return;
		submitSafe(state, ms, queue, camera);
	}

	protected abstract void submitSafe(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera);

	public boolean isInvalid(T be) {
		return !be.hasLevel() || be.getBlockState()
			.getBlock() == Blocks.AIR;
	}

	public boolean shouldCullItem(Vec3 itemPos, Level level) {
		if (level instanceof PonderLevel)
			return false;

		LevelRendererAccessor accessor = (LevelRendererAccessor) Minecraft.getInstance().levelRenderer;
		Frustum frustum = accessor.create$getCapturedFrustum() != null ?
			accessor.create$getCapturedFrustum() :
			accessor.create$getCullingFrustum();

		AABB itemBB = new AABB(
				itemPos.x - 0.25,
				itemPos.y - 0.25,
				itemPos.z - 0.25,
				itemPos.x + 0.25,
				itemPos.y + 0.25,
				itemPos.z + 0.25
		);

		return !frustum.isVisible(itemBB);
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(@NotNull T blockEntity) {
		if (blockEntity instanceof CachedRenderBBBlockEntity cbe)
			return cbe.getRenderBoundingBox();

		return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
	}
}
