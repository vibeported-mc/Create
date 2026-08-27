package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ToolboxSlot extends SlotItemHandler {

	private ToolboxMenu toolboxMenu;
	private boolean isVisible;

	public ToolboxSlot(ToolboxMenu menu, ResourceHandler<ItemResource> itemHandler, int index, int xPosition, int yPosition, boolean isVisible) {
		super(itemHandler, index, xPosition, yPosition);
		this.toolboxMenu = menu;
		this.isVisible = isVisible;
	}

	@Override
	public boolean isActive() {
		return !toolboxMenu.renderPass && super.isActive() && isVisible;
	}
	
	@Override
	public int getMaxStackSize(ItemStack stack) {
		ItemStack maxAdd = stack.copy();
		int maxInput = stack.getMaxStackSize();
		maxAdd.setCount(maxInput);

		ResourceHandler<ItemResource> handler = this.getItemHandler();
		ItemStack currentStack = ItemHandlerHelpers.getStackInSlot(handler, index);
		ItemStack remainder = ItemHandlerHelpers.insertItem(handler, index, maxAdd, true);
		int current = currentStack.getCount();
		int added = maxInput - remainder.getCount();
		return current + added;
	}

}
