package com.simibubi.create.content.contraptions.minecart;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageWrapper;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

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
		private final RootCommitJournal onChange = new RootCommitJournal(TrainCargoManager.this::changeDetected);

		CargoInvWrapper(MountedItemStorageWrapper wrapped) {
			super(wrapped.storages);
		}

		@Override
		public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
			int inserted = super.insert(slot, resource, amount, transaction);
			if (inserted > 0)
				onChange.updateSnapshots(transaction);
			return inserted;
		}

		@Override
		public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
			int extracted = resource.isEmpty() ? 0 : super.extract(slot, resource, amount, transaction);
			if (extracted > 0)
				onChange.updateSnapshots(transaction);
			return extracted;
		}

		@Override
		public void set(int slot, ItemResource resource, int amount) {
			if (!resource.matches(ItemUtil.getStack(this, slot)))
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
			int drained = resource.isEmpty() ? 0 : super.extract(tank, resource, amount, transaction);
			if (drained > 0)
				changeDetected();
			return drained;
		}

	}

}
