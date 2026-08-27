package com.simibubi.create.content.equipment.zapper;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ZapperItemRenderer extends CustomRenderedItemModelRenderer {

	@Override
	protected void render(ItemStack stack, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
		PoseStack ms, SubmitNodeCollector buffer, int light, int overlay) {
		// Block indicator
		if (transformType == ItemDisplayContext.GUI && stack.has(AllDataComponents.SHAPER_BLOCK_USED))
			renderBlockUsed(stack, ms, buffer, light, overlay);
	}

	/**
	 * The block the shaper is set to, shown as a small item in the corner of its icon.
	 * <p>
	 * 26.2 draws items from a render state rather than from a model handed in, so the block's own
	 * item is resolved and submitted; the fence-and-wall special case the old code carried is gone
	 * with it, since an item model is what gets resolved either way.
	 */
	private void renderBlockUsed(ItemStack stack, PoseStack ms, SubmitNodeCollector buffer, int light, int overlay) {
		BlockState state = stack.get(AllDataComponents.SHAPER_BLOCK_USED);
		if (state == null)
			return;

		ms.pushPose();
		ms.translate(-0.3F, -0.45F, -0.0F);
		ms.scale(0.25F, 0.25F, 0.25F);

		ItemStackRenderState blockItem = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(blockItem, new ItemStack(state.getBlock()), ItemDisplayContext.NONE, null, null, 0);
		blockItem.submit(ms, buffer, light, overlay, 0);
		ms.popPose();
	}

	protected float getAnimationProgress(float pt, boolean leftHanded, boolean mainHand) {
		float animation = CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded, pt);
		return Mth.clamp(animation * 5, 0, 1);
	}

}
