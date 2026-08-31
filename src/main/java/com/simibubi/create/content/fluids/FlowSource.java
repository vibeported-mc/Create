package com.simibubi.create.content.fluids;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.lang.ref.WeakReference;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.math.BlockFace;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
public abstract class FlowSource {

	private static final ICapabilityProvider<ResourceHandler<FluidResource>> EMPTY = null;

	BlockFace location;

	public FlowSource(BlockFace location) {
		this.location = location;
	}

	public FluidStack provideFluid(Predicate<FluidStack> extractionPredicate) {
		@Nullable ICapabilityProvider<ResourceHandler<FluidResource>> tankCache = provideHandler();
		if (tankCache == null)
			return FluidStack.EMPTY;
		ResourceHandler<FluidResource> tank = tankCache.getCapability();
		if (tank == null)
			return FluidStack.EMPTY;
		FluidStack immediateFluid = FluidStack.EMPTY;

		for (int i = 0; i < tank.size(); i++) {
			FluidResource held = tank.getResource(i);
			if (held.isEmpty())
				continue;

			// never committed: this only looks at what could be taken
			try (Transaction transaction = Transaction.openRoot()) {
				int drained = tank.extract(held, 1, transaction);
				if (drained <= 0)
					continue;
				immediateFluid = held.toStack(drained);
			}

			break;
		}
		if (extractionPredicate.test(immediateFluid))
			return immediateFluid;

		for (int i = 0; i < tank.size(); i++) {
			FluidStack contained = FluidUtil.getStack(tank, i);
			if (contained.isEmpty())
				continue;
			if (!extractionPredicate.test(contained))
				continue;
			FluidStack toExtract = contained.copy();
			toExtract.setAmount(1);
			try (Transaction transaction = Transaction.openRoot()) {
				FluidResource wanted = FluidResource.of(toExtract);
				int transferred = tank.extract(wanted, toExtract.getAmount(), transaction);
				// simulated: the transaction is dropped, so nothing was really taken
				return transferred <= 0 ? FluidStack.EMPTY : wanted.toStack(transferred);
			}
		}

		return FluidStack.EMPTY;
	}

	// Layer III. PFIs need active attention to prevent them from disengaging early
	public void keepAlive() {}

	public abstract boolean isEndpoint();

	public void manageSource(Level world, BlockEntity networkBE) {
	}

	public void whileFlowPresent(Level world, boolean pulling) {}

	public @Nullable ICapabilityProvider<ResourceHandler<FluidResource>> provideHandler() {
		return EMPTY;
	}

	public static class FluidHandler extends FlowSource {
		@Nullable
		ICapabilityProvider<ResourceHandler<FluidResource>> fluidHandlerCache;

		public FluidHandler(BlockFace location) {
			super(location);
			fluidHandlerCache = EMPTY;
		}

		public void manageSource(Level level, BlockEntity networkBE) {
			if (fluidHandlerCache == null) {
				BlockEntity blockEntity = level.getBlockEntity(location.getConnectedPos());
				if (blockEntity != null) {
					if (level instanceof ServerLevel serverLevel) {
						fluidHandlerCache = ICapabilityProvider.of((invalidate) -> BlockCapabilityCache.create(
							Capabilities.Fluid.BLOCK,
							serverLevel,
							blockEntity.getBlockPos(),
							location.getOppositeFace(),
							() -> !networkBE.isRemoved(),
							() -> {
								fluidHandlerCache = EMPTY;
								invalidate.run();
							}
						));
					} else if (level instanceof PonderLevel) {
						fluidHandlerCache = ICapabilityProvider.of(() -> level.getCapability(
							Capabilities.Fluid.BLOCK,
							blockEntity.getBlockPos(),
							location.getOppositeFace()
						));
					}
				}
			}
		}

		@Override
		@Nullable
		public ICapabilityProvider<ResourceHandler<FluidResource>> provideHandler() {
			return fluidHandlerCache;
		}

		@Override
		public boolean isEndpoint() {
			return true;
		}
	}

	public static class OtherPipe extends FlowSource {
		WeakReference<FluidTransportBehaviour> cached;

		public OtherPipe(BlockFace location) {
			super(location);
		}

		@Override
		public void manageSource(Level world, BlockEntity networkBE) {
			if (cached != null && cached.get() != null && !cached.get().blockEntity.isRemoved())
				return;
			cached = null;
			FluidTransportBehaviour fluidTransportBehaviour =
				BlockEntityBehaviour.get(world, location.getConnectedPos(), FluidTransportBehaviour.TYPE);
			if (fluidTransportBehaviour != null)
				cached = new WeakReference<>(fluidTransportBehaviour);
		}

		@Override
		public FluidStack provideFluid(Predicate<FluidStack> extractionPredicate) {
			if (cached == null || cached.get() == null)
				return FluidStack.EMPTY;
			FluidTransportBehaviour behaviour = cached.get();
			FluidStack providedOutwardFluid = behaviour.getProvidedOutwardFluid(location.getOppositeFace());
			return extractionPredicate.test(providedOutwardFluid) ? providedOutwardFluid : FluidStack.EMPTY;
		}

		@Override
		public boolean isEndpoint() {
			return false;
		}

	}

	public static class Blocked extends FlowSource {

		public Blocked(BlockFace location) {
			super(location);
		}

		@Override
		public boolean isEndpoint() {
			return false;
		}

	}

}
