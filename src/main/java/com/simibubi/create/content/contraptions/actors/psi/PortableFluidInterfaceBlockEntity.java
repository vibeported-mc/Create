package com.simibubi.create.content.contraptions.actors.psi;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
public class PortableFluidInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

	protected ResourceHandler<FluidResource> capability;

	public PortableFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = createEmptyHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Fluid.BLOCK,
				AllBlockEntityTypes.PORTABLE_FLUID_INTERFACE.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void startTransferringTo(Contraption contraption, float distance) {;
		capability = new InterfaceFluidHandler(contraption.getStorage().getFluids());
		invalidateCapability();
		super.startTransferringTo(contraption, distance);
	}

	@Override
	protected void invalidateCapability() {
		invalidateCapabilities();
	}

	@Override
	protected void stopTransferring() {
		capability = createEmptyHandler();
		invalidateCapability();
		super.stopTransferring();
	}

	private ResourceHandler<FluidResource> createEmptyHandler() {
		return new InterfaceFluidHandler(new FluidStacksResourceHandler(0));
	}

	public class InterfaceFluidHandler implements ResourceHandler<FluidResource> {

		private ResourceHandler<FluidResource> wrapped;

		public InterfaceFluidHandler(ResourceHandler<FluidResource> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public FluidResource getResource(int tank) {
			return wrapped.getResource(tank);
		}

		@Override
		public long getAmountAsLong(int tank) {
			return wrapped.getAmountAsLong(tank);
		}

		@Override
		public long getCapacityAsLong(int tank, FluidResource resource) {
			return wrapped.getCapacityAsLong(tank, resource);
		}

		@Override
		public boolean isValid(int tank, FluidResource resource) {
			return wrapped.isValid(tank, resource);
		}

		@Override
		public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			if (!isConnected())
				return 0;
			int filled = wrapped.insert(tank, resource, amount, transaction);
			if (filled > 0)
				keepAlive();
			return filled;
		}

		@Override
		public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			if (!canTransfer())
				return 0;
			int drained = wrapped.extract(tank, resource, amount, transaction);
			if (drained > 0)
				keepAlive();
			return drained;
		}

		public void keepAlive() {
			onContentTransferred();
		}

	}

}
