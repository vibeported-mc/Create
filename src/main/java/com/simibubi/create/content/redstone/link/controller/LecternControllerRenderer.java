package com.simibubi.create.content.redstone.link.controller;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.item.render.CustomItemRenderContext;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class LecternControllerRenderer
	extends SafeBlockEntityRenderer<LecternControllerBlockEntity, LecternControllerRenderer.LecternRenderState> {

	public static class LecternRenderState extends SafeRenderState {
		public @Nullable Direction facing;
		public boolean active;
		public boolean renderDepression;
	}

	public LecternControllerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public LecternRenderState createRenderState() {
		return new LecternRenderState();
	}

	@Override
	protected void extractSafe(LecternControllerBlockEntity be, LecternRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.facing = be.getBlockState()
			.getValue(LecternControllerBlock.FACING);
		state.active = be.hasUser();
		state.renderDepression = be.isUsedBy(Minecraft.getInstance().player);
	}

	@Override
	protected void submitSafe(LecternRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.facing == null)
			return;

		ItemStack stack = AllItems.LINKED_CONTROLLER.asStack();
		ItemDisplayContext transformType = ItemDisplayContext.NONE;
		PartialItemModelRenderer renderer = PartialItemModelRenderer
			.of(new CustomItemRenderContext(stack, transformType, null, null, null, 0), ms, queue,
				OverlayTexture.NO_OVERLAY);

		var msr = TransformStack.of(ms);

		ms.pushPose();
		msr.translate(0.5, 1.45, 0.5);
		msr.rotateYDegrees(AngleHelper.horizontalAngle(state.facing) - 90);
		msr.translate(0.28, 0, 0);
		msr.rotateZDegrees(-22.0f);
		LinkedControllerItemRenderer.renderInLectern(stack, renderer, transformType, ms, state.lightCoords,
			state.active, state.renderDepression);
		ms.popPose();
	}

}
