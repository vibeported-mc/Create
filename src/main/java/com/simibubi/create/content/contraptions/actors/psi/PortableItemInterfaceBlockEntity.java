package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
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
		return new InterfaceItemHandler(new ItemStacksResourceHandler(0));
	}

	@Override
	protected void invalidateCapability() {
		invalidateCapabilities();
	}

	class InterfaceItemHandler extends ItemHandlerWrapper {

		public InterfaceItemHandler(ModifiableItemHandler wrapped) {
			super(wrapped);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!canTransfer())
				return ItemStack.EMPTY;
			ItemStack extractItem = super.extractItem(slot, amount, simulate);
			if (!simulate && !extractItem.isEmpty())
				onContentTransferred();
			return extractItem;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!canTransfer())
				return stack;
			ItemStack insertItem = super.insertItem(slot, stack, simulate);
			if (!simulate && !ItemStack.matches(insertItem, stack))
				onContentTransferred();
			return insertItem;
		}

	}

}
