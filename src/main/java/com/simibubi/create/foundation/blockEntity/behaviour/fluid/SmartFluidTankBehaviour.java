package com.simibubi.create.foundation.blockEntity.behaviour.fluid;

import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.ResourceHandler;
import com.simibubi.create.foundation.transfer.Transactions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
public class SmartFluidTankBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<SmartFluidTankBehaviour>

	TYPE = new BehaviourType<>(), INPUT = new BehaviourType<>("Input"), OUTPUT = new BehaviourType<>("Output");

	private static final int SYNC_RATE = 8;

	protected int syncCooldown;
	protected boolean queuedSync;
	protected TankSegment[] tanks;
	protected ResourceHandler<FluidResource> capability;
	protected boolean extractionAllowed;
	protected boolean insertionAllowed;
	protected Runnable fluidUpdateCallback;

	private BehaviourType<SmartFluidTankBehaviour> behaviourType;

	public static SmartFluidTankBehaviour single(SmartBlockEntity be, int capacity) {
		return new SmartFluidTankBehaviour(TYPE, be, 1, capacity, false);
	}

	public SmartFluidTankBehaviour(BehaviourType<SmartFluidTankBehaviour> type, SmartBlockEntity be, int tanks,
		int tankCapacity, boolean enforceVariety) {
		super(be);
		insertionAllowed = true;
		extractionAllowed = true;
		behaviourType = type;
		this.tanks = new TankSegment[tanks];
		// Java forbids creating a generic array directly.
		@SuppressWarnings("unchecked")
		ResourceHandler<FluidResource>[] handlers = new ResourceHandler[tanks];
		for (int i = 0; i < tanks; i++) {
			TankSegment tankSegment = new TankSegment(tankCapacity);
			this.tanks[i] = tankSegment;
			handlers[i] = tankSegment.tank;
		}
		capability = new InternalFluidHandler(handlers, enforceVariety);
		fluidUpdateCallback = () -> {
		};
	}

	public SmartFluidTankBehaviour whenFluidUpdates(Runnable fluidUpdateCallback) {
		this.fluidUpdateCallback = fluidUpdateCallback;
		return this;
	}

	public SmartFluidTankBehaviour allowInsertion() {
		insertionAllowed = true;
		return this;
	}

	public SmartFluidTankBehaviour allowExtraction() {
		extractionAllowed = true;
		return this;
	}

	public SmartFluidTankBehaviour forbidInsertion() {
		insertionAllowed = false;
		return this;
	}

	public SmartFluidTankBehaviour forbidExtraction() {
		extractionAllowed = false;
		return this;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (getWorld().isClientSide())
			return;
		forEach(ts -> {
			ts.fluidLevel.forceNextSync();
			ts.onFluidStackChanged();
		});
	}

	@Override
	public void tick() {
		super.tick();

		if (syncCooldown > 0) {
			syncCooldown--;
			if (syncCooldown == 0 && queuedSync)
				updateFluids();
		}

		forEach(be -> {
			LerpedFloat fluidLevel = be.getFluidLevel();
			if (fluidLevel != null)
				fluidLevel.tickChaser();
		});
	}

	public void sendDataImmediately() {
		syncCooldown = 0;
		queuedSync = false;
		updateFluids();
	}

	public void sendDataLazily() {
		if (syncCooldown > 0) {
			queuedSync = true;
			return;
		}
		updateFluids();
		queuedSync = false;
		syncCooldown = SYNC_RATE;
	}

	protected void updateFluids() {
		fluidUpdateCallback.run();
		blockEntity.sendData();
		blockEntity.setChanged();
	}

	@Override
	public void unload() {
		super.unload();
		blockEntity.getLevel().invalidateCapabilities(getPos());
	}

	public SmartFluidTank getPrimaryHandler() {
		return getPrimaryTank().tank;
	}

	public TankSegment getPrimaryTank() {
		return tanks[0];
	}

	public TankSegment[] getTanks() {
		return tanks;
	}

	public boolean isEmpty() {
		for (TankSegment tankSegment : tanks)
			if (!tankSegment.tank.isEmpty())
				return false;
		return true;
	}

	public void forEach(Consumer<TankSegment> action) {
		for (TankSegment tankSegment : tanks)
			action.accept(tankSegment);
	}

	public ResourceHandler<FluidResource> getCapability() {
		return capability;
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(nbt, registries, clientPacket);
		ListTag tanksNBT = new ListTag();
		forEach(ts -> tanksNBT.add(ts.writeNBT(registries)));
		nbt.put(getType().getName() + "Tanks", tanksNBT);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(nbt, registries, clientPacket);
		MutableInt index = new MutableInt(0);
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty(getType().getName() + "Tanks"), c -> {
			if (index.intValue() >= tanks.length)
				return;
			tanks[index.intValue()].readNBT(c, registries, clientPacket);
			index.increment();
		});
	}

	public class InternalFluidHandler extends CombinedTankWrapper {

		public InternalFluidHandler(ResourceHandler<FluidResource>[] handlers, boolean enforceVariety) {
			super(handlers);
			if (enforceVariety)
				enforceVariety();
		}

		@Override
		public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			if (!insertionAllowed)
				return 0;
			return super.insert(tank, resource, amount, transaction);
		}

		@Override
		public int insert(FluidResource resource, int amount, TransactionContext transaction) {
			if (!insertionAllowed)
				return 0;
			return super.insert(resource, amount, transaction);
		}

		/**
		 * Fill past the insertion lock, for the machine that owns the tank rather than a pipe
		 * attached to it.
		 */
		public int forceFill(FluidStack resource, boolean simulate) {
			try (Transaction transaction = Transactions.open()) {
				int filled = super.insert(FluidResource.of(resource), resource.getAmount(), transaction);
				if (!simulate)
					transaction.commit();
				return filled;
			}
		}

		@Override
		public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			if (!extractionAllowed)
				return 0;
			return super.extract(tank, resource, amount, transaction);
		}

		@Override
		public int extract(FluidResource resource, int amount, TransactionContext transaction) {
			if (!extractionAllowed)
				return 0;
			return super.extract(resource, amount, transaction);
		}

	}

	public class TankSegment {

		protected SmartFluidTank tank;
		protected LerpedFloat fluidLevel;
		protected FluidStack renderedFluid;

		public TankSegment(int capacity) {
			tank = new SmartFluidTank(capacity, f -> onFluidStackChanged());
			fluidLevel = LerpedFloat.linear()
				.startWithValue(0)
				.chase(0, .25, Chaser.EXP);
			renderedFluid = FluidStack.EMPTY;
		}

		public void onFluidStackChanged() {
			if (!blockEntity.hasLevel())
				return;
			fluidLevel.chase(tank.getFluidAmount() / (float) tank.getCapacity(), .25, Chaser.EXP);
			if (!getWorld().isClientSide())
				sendDataLazily();
			if (blockEntity.isVirtual() && !tank.getFluid()
				.isEmpty())
				renderedFluid = tank.getFluid();
		}

		public FluidStack getRenderedFluid() {
			return renderedFluid;
		}

		public LerpedFloat getFluidLevel() {
			return fluidLevel;
		}

		public float getTotalUnits(float partialTicks) {
			return fluidLevel.getValue(partialTicks) * tank.getCapacity();
		}

		public CompoundTag writeNBT(HolderLookup.Provider registries) {
			CompoundTag compound = new CompoundTag();
			compound.put("TankContent", tank.writeToNBT(registries, new CompoundTag()));
			compound.put("Level", NbtValueIO.toTag(fluidLevel::write));
			return compound;
		}

		public void readNBT(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
			tank.readFromNBT(registries, compound.getCompoundOrEmpty("TankContent"));
			fluidLevel.read(NbtValueIO.fromTag(compound.getCompoundOrEmpty("Level")), clientPacket);
			if (!tank.getFluid()
				.isEmpty())
				renderedFluid = tank.getFluid();
		}

		public boolean isEmpty(float partialTicks) {
			FluidStack renderedFluid = getRenderedFluid();
			if (renderedFluid.isEmpty())
				return true;
			float units = getTotalUnits(partialTicks);
			if (units < 1)
				return true;
			return false;
		}

	}

	@Override
	public BehaviourType<?> getType() {
		return behaviourType;
	}
}
