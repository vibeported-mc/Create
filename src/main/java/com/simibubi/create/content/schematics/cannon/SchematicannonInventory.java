package com.simibubi.create.content.schematics.cannon;

import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
public class SchematicannonInventory extends ItemStacksResourceHandler {
	private final SchematicannonBlockEntity blockEntity;

	public SchematicannonInventory(SchematicannonBlockEntity blockEntity) {
		super(5);
		this.blockEntity = blockEntity;
	}

	@Override
	protected void onContentsChanged(int slot, ItemStack previousContents) {
		super.onContentsChanged(slot, previousContents);
		blockEntity.setChanged();
	}

	@Override
	public boolean isValid(int slot, ItemResource resource) {
		ItemStack stack = resource.toStack(1);
		switch (slot) {
		case 0: // Blueprint Slot
			return AllItems.SCHEMATIC.isIn(stack);
		case 1: // Blueprint output
			return false;
		case 2: // Book input
			return AllBlocks.CLIPBOARD.isIn(stack) || stack.is(Items.BOOK)
				|| stack.is(Items.WRITTEN_BOOK);
		case 3: // Material List output
			return false;
		case 4: // Gunpowder
			return stack.is(Items.GUNPOWDER);
		default:
			return super.isValid(slot, resource);
		}
	}
}
