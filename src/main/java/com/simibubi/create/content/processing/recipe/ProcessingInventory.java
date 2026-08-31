package com.simibubi.create.content.processing.recipe;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ProcessingInventory extends ItemStacksResourceHandler {
	public float remainingTime;
	public float recipeDuration;
	public boolean appliedRecipe;
	public Consumer<ItemStack> callback;
	private boolean limit;

	public ProcessingInventory(Consumer<ItemStack> callback) {
		super(32);
		this.callback = callback;
	}

	public ProcessingInventory withSlotLimit(boolean limit) {
		this.limit = limit;
		return this;
	}

	@Override
	protected int getCapacity(int index, ItemResource resource) {
		return !limit ? super.getCapacity(index, resource) : 1;
	}

	public void clear() {
		for (int i = 0; i < size(); i++)
			set(i, ItemResource.EMPTY, 0);
		remainingTime = 0;
		recipeDuration = 0;
		appliedRecipe = false;
	}

	public boolean isEmpty() {
		for (int i = 0; i < size(); i++)
			if (!getResource(i).isEmpty())
				return false;
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		int inserted = super.insert(index, resource, amount, transaction);
		if (index == 0 && inserted > 0)
			callback.accept(ItemUtil.getStack(this, index));
		return inserted;
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		output.putFloat("ProcessingTime", remainingTime);
		output.putFloat("RecipeTime", recipeDuration);
		output.putBoolean("AppliedRecipe", appliedRecipe);
	}

	@Override
	public void deserialize(ValueInput input) {
		remainingTime = input.getFloatOr("ProcessingTime", 0);
		recipeDuration = input.getFloatOr("RecipeTime", 0);
		appliedRecipe = input.getBooleanOr("AppliedRecipe", false);
		super.deserialize(input);
		if (isEmpty())
			appliedRecipe = false;
	}

	/**
	 * Processing inventories are filled by the machine and emptied by its own logic, never pulled from.
	 */
	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return index == 0 && isEmpty();
	}

}
