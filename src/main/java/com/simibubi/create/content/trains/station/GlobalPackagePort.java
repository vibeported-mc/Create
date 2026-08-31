package com.simibubi.create.content.trains.station;

import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.simibubi.create.Create;

public class GlobalPackagePort {
	public String address = "";
	public ItemStacksResourceHandler offlineBuffer = new ItemStacksResourceHandler(18);
	public boolean primed = false;
	private boolean restoring = false;

	public void restoreOfflineBuffer(IndexModifier<ItemResource> inventory) {
		if (!primed) return;

		restoring = true;

		for (int slot = 0; slot < offlineBuffer.size(); slot++) {
			ItemStack stack = ItemUtil.getStack(offlineBuffer, slot);
			inventory.set(slot, ItemResource.of(stack), stack.getCount());
		}

		restoring = false;
		primed = false;
	}

	public void saveOfflineBuffer(ResourceHandler<ItemResource> inventory) {
		/*
		 * Each time restoreOfflineBuffer changes a slot, the inventory
		 * calls this method. We must filter out those calls to prevent
		 * overwriting later slots which haven't been restored yet and
		 * to avoid unnecessary work.
		 */
		if (restoring) return;

		// TODO: Call save method on individual slots rather than iterating
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = ItemUtil.getStack(inventory, slot);
			offlineBuffer.set(slot, ItemResource.of(stack), stack.getCount());
		}

		Create.RAILWAYS.markTracksDirty();
	}
}
