package com.simibubi.create.compat.thresholdSwitch;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.compat.Mods;

import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SophisticatedStorage implements ThresholdSwitchCompat {

	@Override
	public boolean isFromThisMod(BlockEntity be) {
		if (be == null)
			return false;

		String namespace = RegisteredObjectsHelper.getKeyOrThrow(be.getType())
			.getNamespace();

		return
			Mods.SOPHISTICATEDSTORAGE.id().equals(namespace)
			|| Mods.SOPHISTICATEDBACKPACKS.id().equals(namespace);
	}

	@Override
	public long getSpaceInSlot(ResourceHandler<ItemResource> inv, int slot) {
		return (inv.getCapacityAsInt(slot, ItemResource.EMPTY) * ItemUtil.getStack(inv, slot).getOrDefault(DataComponents.MAX_STACK_SIZE, 64)) / 64;
	}

}
