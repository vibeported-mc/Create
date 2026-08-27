package com.simibubi.create.content.equipment.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CardboardSwordItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel HELD = PartialModel.of(Create.asResource("item/cardboard_sword/item_in_hand"));

	@Override
	protected void render(ItemStack stack, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
		PoseStack ms, SubmitNodeCollector buffer, int light, int overlay) {
		renderer.render(transformType == ItemDisplayContext.GUI ? model.getOriginalModel() : HELD.get(), light);
	}

}
