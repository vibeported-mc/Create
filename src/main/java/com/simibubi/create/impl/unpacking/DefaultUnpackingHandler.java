package com.simibubi.create.impl.unpacking;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
public enum DefaultUnpackingHandler implements UnpackingHandler {
	INSTANCE;

	@Override
	public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate, @Nullable TransactionContext parent) {
		BlockEntity targetBE = level.getBlockEntity(pos);
		if (targetBE == null)
			return false;

		ResourceHandler<ItemResource> targetInv = level.getCapability(Capabilities.Item.BLOCK, pos, state, targetBE, side);
		if (targetInv == null)
			return false;

		if (!simulate) {
			/*
			 * Some mods do not support slot-by-slot precision during simulate = false.
			 * Faulty interactions may lead to voiding of items, but the simulate pass should
			 * already have correctly identified there to be enough space for everything.
			 */
			for (ItemStack itemStack : items)
				try (Transaction transaction = Transaction.open(parent)) {
					int transferred = itemStack.copy().isEmpty() ? 0 : ResourceHandlerUtil.insertStacking(targetInv, ItemResource.of(itemStack.copy()), itemStack.copy().getCount(), transaction);
					transaction.commit();
				}
			return true;
		}

		for (int slot = 0; slot < targetInv.size(); slot++) {
			ItemStack itemInSlot = ItemUtil.getStack(targetInv, slot);
			int itemsAddedToSlot = 0;

			for (int boxSlot = 0; boxSlot < items.size(); boxSlot++) {
				ItemStack toInsert = items.get(boxSlot);
				if (toInsert.isEmpty())
					continue;

				boolean nothingWouldFit;

				try (Transaction transaction = Transaction.open(parent)) {
					// never committed: this only asks whether the slot would take any of it
					nothingWouldFit =
						targetInv.insert(slot, ItemResource.of(toInsert), toInsert.getCount(), transaction) == 0;
				}

				if (nothingWouldFit)
					continue;

				if (itemInSlot.isEmpty()) {
					int maxStackSize = targetInv.getCapacityAsInt(slot, ItemResource.EMPTY);
					if (maxStackSize < toInsert.getCount()) {
						toInsert.shrink(maxStackSize);
						toInsert = toInsert.copyWithCount(maxStackSize);
					} else
						items.set(boxSlot, ItemStack.EMPTY);

					itemInSlot = toInsert;
					try (Transaction transaction = Transaction.open(parent)) {
						int transferred2 = toInsert.isEmpty() ? 0 : targetInv.insert(slot, ItemResource.of(toInsert), toInsert.getCount(), transaction);
						if (!simulate)
							transaction.commit();
					}
					continue;
				}

				if (!ItemStack.isSameItemSameComponents(toInsert, itemInSlot))
					continue;

				int insertedAmount;

				try (Transaction transaction = Transaction.open(parent)) {
					insertedAmount = targetInv.insert(slot, ItemResource.of(toInsert), toInsert.getCount(), transaction);
					if (!simulate)
						transaction.commit();
				}
				int slotLimit = Math.min(itemInSlot.getMaxStackSize(), targetInv.getCapacityAsInt(slot, ItemResource.EMPTY));
				int insertableAmountWithPreviousItems =
					Math.min(toInsert.getCount(), slotLimit - itemInSlot.getCount() - itemsAddedToSlot);

				int added = Math.min(insertedAmount, Math.max(0, insertableAmountWithPreviousItems));
				itemsAddedToSlot += added;

				items.set(boxSlot, toInsert.copyWithCount(toInsert.getCount() - added));
			}
		}

		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				// something failed to be inserted
				return false;
			}
		}

		return true;
	}
}
