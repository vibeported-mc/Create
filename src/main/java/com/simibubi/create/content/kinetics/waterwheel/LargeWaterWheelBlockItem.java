package com.simibubi.create.content.kinetics.waterwheel;

import net.createmod.catnip.api.platform.services.PlatformHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LargeWaterWheelBlockItem extends BlockItem {

	public LargeWaterWheelBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		InteractionResult result = super.place(ctx);
		if (result != InteractionResult.FAIL)
			return result;
		Direction clickedFace = ctx.getClickedFace();
		if (clickedFace.getAxis() != ((LargeWaterWheelBlock) getBlock()).getAxisForPlacement(ctx))
			result = super.place(BlockPlaceContext.at(ctx, ctx.getClickedPos()
				.relative(clickedFace), clickedFace));
		if (result == InteractionResult.FAIL && ctx.getLevel()
			.isClientSide())
			PlatformHelper.INSTANCE.executeOnClientOnly(() -> () -> LargeWaterWheelPlacementHint.show(ctx,
				((LargeWaterWheelBlock) getBlock()).getAxisForPlacement(ctx)));
		return result;
	}

}
