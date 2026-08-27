package com.simibubi.create.foundation.blockEntity.renderer;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.content.redstone.link.LinkRenderer.LinkRenderState;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.FilterRenderState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class SmartBlockEntityRenderer<T extends SmartBlockEntity, S extends SmartBlockEntityRenderer.SmartRenderState>
	extends SafeBlockEntityRenderer<T, S> {

	public static class SmartRenderState extends SafeRenderState {
		public @Nullable FilterRenderState filter;
		public @Nullable LinkRenderState link;
		public @Nullable NameplateRenderState nameplate;
	}

	protected final ItemModelResolver itemModelResolver;

	public SmartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new SmartRenderState();
	}

	@Override
	protected void extractSafe(T be, S state, float partialTicks, Vec3 cameraPosition) {
		state.filter = FilteringRenderer.getFilterRenderState(be, itemModelResolver, cameraPosition);
		state.link = LinkRenderer.getLinkRenderState(be, itemModelResolver, cameraPosition);
	}

	@Override
	protected void submitSafe(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.filter != null)
			state.filter.submit(state.blockState, queue, ms, state.lightCoords);
		if (state.link != null)
			state.link.submit(state.blockState, queue, ms, state.lightCoords);
		if (state.nameplate != null)
			state.nameplate.submit(ms, queue, camera);
	}

	/**
	 * Extract half of the old {@code renderNameplateOnHover}: the hover test reads the player and the
	 * hit result, so it has to happen during extraction. Returns null when nothing should be drawn.
	 */
	@Nullable
	protected NameplateRenderState extractNameplateOnHover(T be, Component tag, float yOffset, Vec3 cameraPosition,
		int light) {
		if (be.isVirtual())
			return null;
		BlockPos pos = be.getBlockPos();
		if (cameraPosition.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0f)
			return null;
		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (!(hitResult instanceof BlockHitResult bhr) || bhr.getType() == Type.MISS || !bhr.getBlockPos()
			.equals(pos))
			return null;
		return new NameplateRenderState(new Vec3(0.5, yOffset - 0.25, 0.5), tag, light);
	}

	/**
	 * Nameplates are no longer drawn by hand: the queue owns the billboard orientation, background and
	 * two-pass see-through drawing that this used to replicate.
	 */
	public record NameplateRenderState(Vec3 pos, Component label, int light) {
		public void submit(PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
			queue.submitNameTag(ms, pos, 0, label, true, light, camera);
		}
	}

}
