package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.item.SmartInventory;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BasinInventory extends SmartInventory {

	private BasinBlockEntity blockEntity;

	public boolean packagerMode;

	public BasinInventory(int slots, BasinBlockEntity be) {
		super(slots, be, 64, true);
		this.blockEntity = be;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (packagerMode) // Unique stack insertion only matters for belt setups
			return inv.insert(index, resource, amount, transaction);

		int firstFreeSlot = -1;

		for (int i = 0; i < size(); i++) {
			// Only insert if no other slot already has a stack of this item
			if (i != index && resource.equals(inv.getResource(i)))
				return 0;
			if (inv.getResource(i)
				.isEmpty() && firstFreeSlot == -1)
				firstFreeSlot = i;
		}

		// Only insert if this is the first empty slot, prevents overfilling in the
		// simulation pass
		if (inv.getResource(index)
			.isEmpty() && firstFreeSlot != index)
			return 0;

		return super.insert(index, resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		int extracted = resource.isEmpty() ? 0 : super.extract(index, resource, amount, transaction);
		if (extracted > 0)
			blockEntity.notifyChangeOfContents();
		return extracted;
	}

}
