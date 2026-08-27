package com.simibubi.create.content.equipment.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class GogglesModel extends BakedModelWrapper<BlockStateModel> {

	public GogglesModel(BlockStateModel template) {
		super(template);
	}

	@Override
	public BlockStateModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat, boolean leftHanded) {
		if (cameraItemDisplayContext == ItemDisplayContext.HEAD)
			return AllPartialModels.GOGGLES.get()
				.applyTransform(cameraItemDisplayContext, mat, leftHanded);
		return super.applyTransform(cameraItemDisplayContext, mat, leftHanded);
	}

}
