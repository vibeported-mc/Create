package com.simibubi.create.compat.thresholdSwitch;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.level.block.entity.BlockEntity;
public interface ThresholdSwitchCompat {

	boolean isFromThisMod(BlockEntity blockEntity);

	long getSpaceInSlot(ResourceHandler<ItemResource> inv, int slot);

}
