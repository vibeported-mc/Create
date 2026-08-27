package com.simibubi.create.foundation.item;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.api.data.IntAttached;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public class CountedItemStackList {

	Map<Item, Set<ItemStackEntry>> items = new HashMap<>();

	public CountedItemStackList(ResourceHandler<ItemResource> inventory, FilteringBehaviour filteringBehaviour) {
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack extractItem = ItemHandlerHelpers.getStackInSlot(inventory, slot);
			if (filteringBehaviour.test(extractItem))
				add(extractItem);
		}
	}

	public Stream<IntAttached<MutableComponent>> getTopNames(int limit) {
		return items.values()
			.stream()
			.flatMap(Collection::stream)
			.sorted(IntAttached.comparator())
			.limit(limit)
			.map(entry -> IntAttached.with(entry.count(), entry.stack()
				.getHoverName()
				.copy()));
	}

	public void add(ItemStack stack) {
		add(stack, stack.getCount());
	}

	public void add(ItemStack stack, int amount) {
		if (stack.isEmpty())
			return;

		Set<ItemStackEntry> stackSet = getOrCreateItemSet(stack);
		for (ItemStackEntry entry : stackSet) {
			if (!entry.matches(stack))
				continue;
			entry.grow(amount);
			return;
		}
		stackSet.add(new ItemStackEntry(stack, amount));
	}

	private Set<ItemStackEntry> getOrCreateItemSet(ItemStack stack) {
		if (!items.containsKey(stack.getItem()))
			items.put(stack.getItem(), new HashSet<>());
		return getItemSet(stack);
	}

	private Set<ItemStackEntry> getItemSet(ItemStack stack) {
		return items.get(stack.getItem());
	}

	public static class ItemStackEntry extends IntAttached<ItemStack> {

		public ItemStackEntry(ItemStack stack) {
			this(stack, stack.getCount());
		}

		public ItemStackEntry(ItemStack stack, int amount) {
			super(amount, stack);
		}

		public boolean matches(ItemStack other) {
			return ItemStack.isSameItemSameComponents(other, stack());
		}

		public ItemStack stack() {
			return getSecond();
		}

		public void grow(int amount) {
			setFirst(getFirst() + amount);
		}

		public int count() {
			return getFirst();
		}

	}

}
