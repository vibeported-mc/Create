package com.simibubi.create.content.equipment.wrench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class WrenchItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel GEAR = PartialModel.of(Create.asResource("item/wrench/gear"));

	@Override
	protected void render(ItemStack stack, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
		PoseStack ms, SubmitNodeCollector buffer, int light, int overlay) {
		renderer.renderBase(light);

		float xOffset = -1/16f;
		ms.translate(-xOffset, 0, 0);
		ms.mulPose(Axis.YP.rotationDegrees(ScrollValueHandler.getScroll(AnimationTickHolder.getPartialTicks())));
		ms.translate(xOffset, 0, 0);

		renderer.render(GEAR.get(), light);
	}

}
