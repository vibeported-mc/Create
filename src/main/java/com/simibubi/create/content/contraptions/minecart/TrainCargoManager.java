package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.foundation.item.CommitCallback;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageWrapper;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

public class TrainCargoManager extends MountedStorageManager {

	int ticksSinceLastExchange;
	AtomicInteger version;

	public TrainCargoManager() {
		version = new AtomicInteger();
		ticksSinceLastExchange = 0;
	}

	@Override
	public void initialize() {
		super.initialize();
		this.items = new CargoInvWrapper(this.items);
		this.allItems = this.items;
		if (this.fuelItems != null) {
			this.fuelItems = new CargoInvWrapper(this.fuelItems);
		}
		this.fluids = new CargoTankWrapper(this.fluids);
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(nbt, registries, clientPacket);
		nbt.putInt("TicksSinceLastExchange", ticksSinceLastExchange);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, @Nullable Contraption contraption) {
		super.read(nbt, registries, clientPacket, contraption);
		ticksSinceLastExchange = nbt.getIntOr("TicksSinceLastExchange", 0);
	}

	public void resetIdleCargoTracker() {
		ticksSinceLastExchange = 0;
	}

	public void tickIdleCargoTracker() {
		ticksSinceLastExchange++;
	}

	public int getTicksSinceLastExchange() {
		return ticksSinceLastExchange;
	}

	public int getVersion() {
		return version.get();
	}

	void changeDetected() {
		version.incrementAndGet();
		resetIdleCargoTracker();
	}

	class CargoInvWrapper extends MountedItemStorageWrapper {
		private final CommitCallback onChange = new CommitCallback(TrainCargoManager.this::changeDetected);

		CargoInvWrapper(MountedItemStorageWrapper wrapped) {
			super(wrapped.storages);
		}

		@Override
		public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
			int inserted = super.insert(slot, resource, amount, transaction);
			if (inserted > 0)
				onChange.arm(transaction);
			return inserted;
		}

		@Override
		public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
			int extracted = super.extract(slot, resource, amount, transaction);
			if (extracted > 0)
				onChange.arm(transaction);
			return extracted;
		}

		@Override
		public void set(int slot, ItemResource resource, int amount) {
			if (!resource.matches(ItemHandlerHelpers.getStackInSlot(this, slot)))
				changeDetected();
			super.set(slot, resource, amount);
		}

	}

	class CargoTankWrapper extends MountedFluidStorageWrapper {
		CargoTankWrapper(MountedFluidStorageWrapper wrapped) {
			super(wrapped.storages);
		}

		@Override
		public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			int filled = super.insert(tank, resource, amount, transaction);
			if (filled > 0)
				changeDetected();
			return filled;
		}

		@Override
		public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			int drained = super.extract(tank, resource, amount, transaction);
			if (drained > 0)
				changeDetected();
			return drained;
		}

	}

}
