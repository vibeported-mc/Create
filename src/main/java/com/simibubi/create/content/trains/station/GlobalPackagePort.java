package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import com.simibubi.create.Create;

public class GlobalPackagePort {
	public String address = "";
	public ItemStacksResourceHandler offlineBuffer = new ItemStacksResourceHandler(18);
	public boolean primed = false;
	private boolean restoring = false;

	public void restoreOfflineBuffer(ModifiableItemHandler inventory) {
		if (!primed) return;

		restoring = true;

		for (int slot = 0; slot < offlineBuffer.size(); slot++) {
			ItemHandlerHelpers.setStackInSlot(inventory, slot, ItemHandlerHelpers.getStackInSlot(offlineBuffer, slot));
		}

		restoring = false;
		primed = false;
	}

	public void saveOfflineBuffer(ModifiableItemHandler inventory) {
		/*
		 * Each time restoreOfflineBuffer changes a slot, the inventory
		 * calls this method. We must filter out those calls to prevent
		 * overwriting later slots which haven't been restored yet and
		 * to avoid unnecessary work.
		 */
		if (restoring) return;

		// TODO: Call save method on individual slots rather than iterating
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemHandlerHelpers.setStackInSlot(offlineBuffer, slot, ItemHandlerHelpers.getStackInSlot(inventory, slot));
		}

		Create.RAILWAYS.markTracksDirty();
	}
}
