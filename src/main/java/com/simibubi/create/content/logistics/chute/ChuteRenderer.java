package com.simibubi.create.content.logistics.chute;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.chute.ChuteBlock.Shape;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChuteRenderer extends SafeBlockEntityRenderer<ChuteBlockEntity, ChuteRenderer.ChuteRenderState> {

	public static class ChuteRenderState extends SafeRenderState {
		public @Nullable ItemRenderState item;
	}

	/**
	 * Everything the item draw needs, resolved while the block entity is still available.
	 */
	public record ItemRenderState(ItemStackRenderState item, float itemPosition, boolean isPackage) {
		public void submit(PoseStack ms, SubmitNodeCollector queue, int light) {
			var msr = TransformStack.of(ms);
			ms.pushPose();
			msr.center();
			float itemScale = .5f;
			ms.translate(0, -.5 + itemPosition, 0);
			if (isPackage) {
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(itemScale, itemScale, itemScale);
				msr.rotateXDegrees(itemPosition * 180);
				msr.rotateYDegrees(itemPosition * 180);
			}
			item.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	protected final ItemModelResolver itemModelResolver;

	public ChuteRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public ChuteRenderState createRenderState() {
		return new ChuteRenderState();
	}

	@Override
	protected void extractSafe(ChuteBlockEntity be, ChuteRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.item = null;
		if (be.item.isEmpty())
			return;
		BlockState blockState = be.getBlockState();
		if (blockState.getValue(ChuteBlock.FACING) != Direction.DOWN)
			return;
		if (blockState.getValue(ChuteBlock.SHAPE) != Shape.WINDOW
			&& (be.bottomPullDistance == 0 || be.itemPosition.getValue(partialTicks) > .5f))
			return;

		state.item = extractItem(be, partialTicks, itemModelResolver);
	}

	@Override
	protected void submitSafe(ChuteRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.item != null)
			state.item.submit(ms, queue, state.lightCoords);
	}

	public static ItemRenderState extractItem(ChuteBlockEntity be, float partialTicks,
		ItemModelResolver itemModelResolver) {
		ItemStackRenderState item = new ItemStackRenderState();
		itemModelResolver.updateForTopItem(item, be.item, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
		return new ItemRenderState(item, be.itemPosition.getValue(partialTicks), PackageItem.isPackage(be.item));
	}

}
