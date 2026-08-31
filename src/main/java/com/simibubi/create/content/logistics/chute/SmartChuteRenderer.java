package com.simibubi.create.content.logistics.chute;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.chute.ChuteRenderer.ItemRenderState;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

public class SmartChuteRenderer
	extends SmartBlockEntityRenderer<SmartChuteBlockEntity, SmartChuteRenderer.SmartChuteRenderState> {

	public static class SmartChuteRenderState extends SmartRenderState {
		public @Nullable ItemRenderState item;
	}

	public SmartChuteRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public SmartChuteRenderState createRenderState() {
		return new SmartChuteRenderState();
	}

	@Override
	protected void extractSafe(SmartChuteBlockEntity be, SmartChuteRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.item = null;
		if (be.item.isEmpty())
			return;
		if (be.itemPosition.getValue(partialTicks) > 0)
			return;
		state.item = ChuteRenderer.extractItem(be, partialTicks, itemModelResolver);
	}

	@Override
	protected void submitSafe(SmartChuteRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.item != null)
			state.item.submit(ms, queue, state.lightCoords);
	}

}
