package com.simibubi.create.content.contraptions.actors.psi;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
public class PortableItemInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

	protected ResourceHandler<ItemResource> capability;

	public PortableItemInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = createEmptyHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Item.BLOCK,
				AllBlockEntityTypes.PORTABLE_STORAGE_INTERFACE.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void startTransferringTo(Contraption contraption, float distance) {
		capability = new InterfaceItemHandler(contraption.getStorage().getAllItems());
		invalidateCapability();
        if (level != null && !level.isClientSide())
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.startTransferringTo(contraption, distance);
	}

	@Override
	protected void stopTransferring() {
		capability = createEmptyHandler();
		invalidateCapability();
        if (level != null && !level.isClientSide())
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		super.stopTransferring();
	}

	private ResourceHandler<ItemResource> createEmptyHandler() {
		return new InterfaceItemHandler(new ItemStacksResourceHandler(0));
	}

	@Override
	protected void invalidateCapability() {
		invalidateCapabilities();
	}

	class InterfaceItemHandler extends ItemHandlerWrapper {

		private final RootCommitJournal transferred =
			new RootCommitJournal(PortableItemInterfaceBlockEntity.this::onContentTransferred);

		public InterfaceItemHandler(ResourceHandler<ItemResource> wrapped) {
			super(wrapped);
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!canTransfer())
				return 0;
			int extracted = resource.isEmpty() ? 0 : super.extract(index, resource, amount, transaction);
			if (extracted > 0)
				afterTransfer(transaction);
			return extracted;
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!canTransfer())
				return 0;
			int inserted = super.insert(index, resource, amount, transaction);
			if (inserted > 0)
				afterTransfer(transaction);
			return inserted;
		}

		/**
		 * The interface keeps itself open for a while after a transfer, which only counts once the
		 * transaction is actually kept.
		 */
		private void afterTransfer(TransactionContext transaction) {
			transferred.updateSnapshots(transaction);
		}

	}

}
