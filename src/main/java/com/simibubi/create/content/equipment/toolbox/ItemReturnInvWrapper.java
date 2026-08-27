package com.simibubi.create.content.equipment.toolbox;

import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * For inserting items into a players' inventory anywhere except the hotbar
 */
public class ItemReturnInvWrapper implements ResourceHandler<ItemResource> {

	/** The hotbar is the first row of the main inventory. */
	private static final int HOTBAR_SIZE = 9;

	private final ResourceHandler<ItemResource> mainSlots;

	public ItemReturnInvWrapper(Inventory inv) {
		this.mainSlots = PlayerInventoryWrapper.of(inv)
			.getMainSlots();
	}

	@Override
	public int size() {
		return mainSlots.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return mainSlots.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return mainSlots.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return mainSlots.getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return index >= HOTBAR_SIZE && mainSlots.isValid(index, resource);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index < HOTBAR_SIZE)
			return 0;
		return mainSlots.insert(index, resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return mainSlots.extract(index, resource, amount, transaction);
	}

}
