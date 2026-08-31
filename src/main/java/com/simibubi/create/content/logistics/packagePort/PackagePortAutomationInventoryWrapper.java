package com.simibubi.create.content.logistics.packagePort;

import net.neoforged.neoforge.transfer.ResourceHandler;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class PackagePortAutomationInventoryWrapper extends ItemHandlerWrapper {
	private final PackagePortBlockEntity ppbe;

	public PackagePortAutomationInventoryWrapper(ResourceHandler<ItemResource> wrapped, PackagePortBlockEntity ppbe) {
		super(wrapped);
		this.ppbe = ppbe;
	}

	/**
	 * Only packages addressed to this port may be taken out of it.
	 */
	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (!isAddressedHere(resource))
			return 0;
		return super.extract(index, resource, amount, transaction);
	}

	/**
	 * ...and a package already addressed here belongs in the port, not back in the automation.
	 */
	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (!PackageItem.isPackage(resource.toStack(1)))
			return 0;
		if (isAddressedHere(resource))
			return 0;
		return super.insert(index, resource, amount, transaction);
	}

	private boolean isAddressedHere(ItemResource resource) {
		ItemStack stack = resource.toStack(1);
		if (!PackageItem.isPackage(stack))
			return false;
		String filterString = ppbe.getFilterString();
		return filterString != null && PackageItem.matchAddress(stack, filterString);
	}
}
