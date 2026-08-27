package com.simibubi.create.compat.thresholdSwitch;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.compat.Mods;

import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
public class StorageDrawers implements ThresholdSwitchCompat {

	@Override
	public boolean isFromThisMod(BlockEntity blockEntity) {
		return blockEntity != null && Mods.STORAGEDRAWERS.id()
			.equals(RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType())
				.getNamespace());
	}

	@Override
	public long getSpaceInSlot(ResourceHandler<ItemResource> inv, int slot) {
		if (slot == 0)
			return 0;

		return inv.getSlotLimit(slot);
	}
}
