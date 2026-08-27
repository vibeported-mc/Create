package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.foundation.item.ItemStackHandler;
import com.simibubi.create.foundation.item.CommitCallback;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
public class PortableItemInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {

	protected ModifiableItemHandler capability;

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

	private ModifiableItemHandler createEmptyHandler() {
		return new InterfaceItemHandler(new ItemStackHandler(0));
	}

	@Override
	protected void invalidateCapability() {
		invalidateCapabilities();
	}

	class InterfaceItemHandler extends ItemHandlerWrapper {

		private final CommitCallback transferred =
			new CommitCallback(PortableItemInterfaceBlockEntity.this::onContentTransferred);

		public InterfaceItemHandler(ModifiableItemHandler wrapped) {
			super(wrapped);
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!canTransfer())
				return 0;
			int extracted = super.extract(index, resource, amount, transaction);
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
			transferred.arm(transaction);
		}

	}

}
