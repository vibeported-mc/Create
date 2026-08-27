package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import java.util.concurrent.atomic.AtomicInteger;

import com.simibubi.create.foundation.item.ModifiableItemHandler;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class VersionedInventoryWrapper implements ModifiableItemHandler {

	public static final AtomicInteger idGenerator = new AtomicInteger();

	private ModifiableItemHandler inventory;
	private int version;
	private int id;

	public VersionedInventoryWrapper(ModifiableItemHandler inventory) {
		this.id = idGenerator.getAndIncrement();
		this.inventory = inventory;
		this.version = 0;
	}

	public void incrementVersion() {
		version++;
	}

	public int getVersion() {
		return version;
	}

	public int getId() {
		return id;
	}

	//

	@Override
	public int size() {
		return inventory.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return inventory.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return inventory.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return inventory.getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return inventory.isValid(index, resource);
	}

	//

	/**
	 * The version is bumped as soon as a transfer succeeds rather than waiting for the transaction to
	 * commit. Consumers use it only to notice that the inventory may have changed and rescan, so an
	 * extra bump after a rollback costs a redundant scan, never a wrong answer.
	 */
	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		int inserted = inventory.insert(index, resource, amount, transaction);
		if (inserted > 0)
			incrementVersion();
		return inserted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		int extracted = inventory.extract(index, resource, amount, transaction);
		if (extracted > 0)
			incrementVersion();
		return extracted;
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		ItemResource previousResource = inventory.getResource(index);
		long previousAmount = inventory.getAmountAsLong(index);
		inventory.set(index, resource, amount);

		if (resource.equals(previousResource) && amount == previousAmount)
			return;

		incrementVersion();
	}

}
