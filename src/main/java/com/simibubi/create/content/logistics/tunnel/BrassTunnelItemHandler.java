package com.simibubi.create.content.logistics.tunnel;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.world.item.ItemStack;

public class BrassTunnelItemHandler implements ResourceHandler<ItemResource> {

	private BrassTunnelBlockEntity blockEntity;

	public BrassTunnelItemHandler(BrassTunnelBlockEntity be) {
		this.blockEntity = be;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return blockEntity.stackToDistribute;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (!blockEntity.hasDistributionBehaviour()) {
			ResourceHandler<ItemResource> beltCapability = blockEntity.getBeltCapability();
			if (beltCapability == null)
				return stack;
			return beltCapability.insertItem(slot, stack, simulate);
		}

		if (!blockEntity.canTakeItems())
			return stack;

		ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
		if (!simulate)
			blockEntity.setStackToDistribute(stack, null);
		return remainder;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ResourceHandler<ItemResource> beltCapability = blockEntity.getBeltCapability();
		if (beltCapability == null)
			return ItemStack.EMPTY;
		return beltCapability.extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return blockEntity.stackToDistribute.isEmpty() ? 64 : blockEntity.stackToDistribute.getMaxStackSize();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

}
