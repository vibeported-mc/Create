package com.simibubi.create.content.processing.basin;

import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class BasinMovementBehaviour implements MovementBehaviour {
	public Map<String, ItemStacksResourceHandler> getOrReadInventory(MovementContext context) {
		Map<String, ItemStacksResourceHandler> map = new HashMap<>();
		map.put("InputItems", new ItemStacksResourceHandler(9));
		map.put("OutputItems", new ItemStacksResourceHandler(8));
		map.forEach((s, h) -> NbtValueIO.deserialize(h, context.blockEntityData.getCompoundOrEmpty(s), context.world.registryAccess()));
		return map;
	}

	@Override
	public void tick(MovementContext context) {
		MovementBehaviour.super.tick(context);
		if (context.temporaryData == null || (boolean) context.temporaryData) {
			Vec3 facingVec = context.rotation.apply(Vec3.atLowerCornerOf(Direction.UP.getUnitVec3i()));
			facingVec.normalize();
			if (Direction.getApproximateNearest(facingVec.x, facingVec.y, facingVec.z) == Direction.DOWN)
				dump(context, facingVec);
		}
	}

	private void dump(MovementContext context, Vec3 facingVec) {
		getOrReadInventory(context).forEach((key, itemStackHandler) -> {
			for (int i = 0; i < itemStackHandler.size(); i++) {
				if (ItemUtil.getStack(itemStackHandler, i)
					.isEmpty())
					continue;
				ItemEntity itemEntity = new ItemEntity(context.world, context.position.x, context.position.y,
					context.position.z, ItemUtil.getStack(itemStackHandler, i));
				itemEntity.setDeltaMovement(facingVec.scale(.05));
				context.world.addFreshEntity(itemEntity);
				itemStackHandler.set(i, ItemResource.EMPTY, 0);
			}
			context.blockEntityData.put(key, NbtValueIO.serialize(itemStackHandler, context.world.registryAccess()));
		});
		// FIXME: Why are we setting client-side data here?
		if (context.contraption.entity.level().isClientSide()) {
			BlockEntity blockEntity = context.contraption.getBlockEntityClientSide(context.localPos);
			if (blockEntity instanceof BasinBlockEntity)
				((BasinBlockEntity) blockEntity).readOnlyItems(context.blockEntityData, context.world.registryAccess());
		}
		context.temporaryData = false; // did already dump, so can't anymore
	}
}
