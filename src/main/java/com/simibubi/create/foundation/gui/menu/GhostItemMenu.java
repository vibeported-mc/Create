package com.simibubi.create.foundation.gui.menu;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class GhostItemMenu<T> extends MenuBase<T> implements IClearableMenu {

	public ItemStacksResourceHandler ghostInventory;

	protected GhostItemMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected GhostItemMenu(MenuType<?> type, int id, Inventory inv, T contentHolder) {
		super(type, id, inv, contentHolder);
	}

	protected abstract ItemStacksResourceHandler createGhostInventory();

	protected abstract boolean allowRepeats();

	@Override
	protected void initAndReadInventory(T contentHolder) {
		ghostInventory = createGhostInventory();
	}

	@Override
	public void clearContents() {
		for (int i = 0; i < ghostInventory.size(); i++)
			setGhost(i, ItemStack.EMPTY);
	}

	/**
	 * Puts a stack into one of the ghost slots.
	 * <p>
	 * Through the slot rather than into the handler behind it, because a slot over a resource handler
	 * remembers the stack it last handed out and, when told that something changed, writes that
	 * remembered stack back over the handler. Anything written to the handler directly is undone by the
	 * next such notice; going through the slot keeps the two in step.
	 */
	protected void setGhost(int slot, ItemStack stack) {
		getSlot(slot + 36).set(stack);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
		return slotIn.container == playerInventory;
	}

	@Override
	public boolean canDragTo(Slot slotIn) {
		if (allowRepeats())
			return true;
		return slotIn.container == playerInventory;
	}

	@Override
	public void clicked(int slotId, int dragType, ContainerInput clickTypeIn, Player player) {
		if (slotId < 36) {
			super.clicked(slotId, dragType, clickTypeIn, player);
			return;
		}
		if (clickTypeIn == ContainerInput.THROW)
			return;

		ItemStack held = getCarried();
		int slot = slotId - 36;
		if (clickTypeIn == ContainerInput.CLONE) {
			if (player.isCreative() && held.isEmpty()) {
				ItemStack stackInSlot = ItemUtil.getStack(ghostInventory, slot)
						.copy();
				stackInSlot.setCount(stackInSlot.getMaxStackSize());
				setCarried(stackInSlot);
				return;
			}
			return;
		}

		ItemStack insert;
		if (held.isEmpty()) {
			insert = ItemStack.EMPTY;
		} else {
			insert = held.copy();
			insert.setCount(1);
		}
		setGhost(slot, insert);
	}

	@Override
	protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		if (index < 36) {
			Slot slot = this.slots.get(index);
			ItemStack stackToInsert = slot.getItem();
			for (int i = 0; i < ghostInventory.size(); i++) {
				ItemStack stack = ItemUtil.getStack(ghostInventory, i);
				if (!allowRepeats() && ItemStack.isSameItemSameComponents(stack, stackToInsert))
					break;
				if (stack.isEmpty()) {
					ItemStack copy = stackToInsert.copy();
					copy.setCount(1);
					setGhost(i, copy);
					break;
				}
			}
		} else {
			setGhost(index - 36, ItemStack.EMPTY);
		}
		return ItemStack.EMPTY;
	}

}
