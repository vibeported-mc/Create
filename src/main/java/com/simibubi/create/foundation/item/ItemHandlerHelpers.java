package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import com.simibubi.create.foundation.transfer.Transactions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Stack-shaped conveniences over the 26.2 transfer API.
 * <p>
 * The new API works in {@link ItemResource} plus an amount, and every mutation takes a transaction.
 * Create's logic is written in terms of whole {@link ItemStack}s, so these express the handful of
 * operations it performs without each call site having to open its own transaction. Anything that
 * needs to roll back several steps together should open a {@link Transaction} and use the handler
 * directly instead.
 */
public class ItemHandlerHelpers {

	/**
	 * The stack in a slot. The returned stack is a copy - the handler owns its own storage - so
	 * mutating it does not write back.
	 */
	public static ItemStack getStackInSlot(ResourceHandler<ItemResource> handler, int slot) {
		return ItemUtil.getStack(handler, slot);
	}

	/**
	 * Insert into one slot, returning what would not fit.
	 */
	public static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack,
		boolean simulate) {
		return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, Transactions.current());
	}

	/**
	 * Insert anywhere it fits, returning what would not fit.
	 */
	public static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
		return ItemUtil.insertItemReturnRemaining(handler, stack, simulate, Transactions.current());
	}

	/**
	 * Insert anywhere, filling partially occupied slots before empty ones. This is what the old
	 * ItemHandlerHelpers.insertItemStacked did.
	 */
	public static ItemStack insertItemStacked(ResourceHandler<ItemResource> handler, ItemStack stack,
		boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		try (Transaction transaction = Transactions.open()) {
			int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(),
				transaction);
			if (!simulate)
				transaction.commit();
			int leftover = stack.getCount() - inserted;
			return leftover == 0 ? ItemStack.EMPTY : stack.copyWithCount(leftover);
		}
	}

	/**
	 * Take up to {@code amount} out of one slot.
	 */
	public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount,
		boolean simulate) {
		if (amount <= 0)
			return ItemStack.EMPTY;
		ItemResource resource = handler.getResource(slot);
		if (resource.isEmpty())
			return ItemStack.EMPTY;
		try (Transaction transaction = Transactions.open()) {
			int extracted = handler.extract(slot, resource, amount, transaction);
			if (extracted <= 0)
				return ItemStack.EMPTY;
			if (!simulate)
				transaction.commit();
			return resource.toStack(extracted);
		}
	}

	/**
	 * Take up to {@code amount} of a given resource from anywhere in the handler.
	 */
	public static ItemStack extractItem(ResourceHandler<ItemResource> handler, ItemResource resource, int amount,
		boolean simulate) {
		if (amount <= 0 || resource.isEmpty())
			return ItemStack.EMPTY;
		try (Transaction transaction = Transactions.open()) {
			int extracted = handler.extract(resource, amount, transaction);
			if (extracted <= 0)
				return ItemStack.EMPTY;
			if (!simulate)
				transaction.commit();
			return resource.toStack(extracted);
		}
	}

	/**
	 * Overwrite a slot outright.
	 * <p>
	 * Direct mutation lives on {@link IndexModifier}, which {@link StacksResourceHandler} provides as
	 * a plain method without declaring the interface, so both shapes are accepted here rather than
	 * forcing every caller to pick one.
	 */
	public static void setStackInSlot(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
		ItemResource resource = ItemResource.of(stack);
		if (handler instanceof IndexModifier<?> modifier) {
			@SuppressWarnings("unchecked")
			IndexModifier<ItemResource> itemModifier = (IndexModifier<ItemResource>) modifier;
			itemModifier.set(slot, resource, stack.getCount());
			return;
		}
		if (handler instanceof StacksResourceHandler<?, ?> stacks) {
			@SuppressWarnings("unchecked")
			StacksResourceHandler<?, ItemResource> itemStacks = (StacksResourceHandler<?, ItemResource>) stacks;
			itemStacks.set(slot, resource, stack.getCount());
			return;
		}
		throw new UnsupportedOperationException(
			handler.getClass() + " does not support overwriting slots directly");
	}

	/**
	 * The slot's own limit, independent of what it holds.
	 */
	public static int getSlotLimit(ResourceHandler<ItemResource> handler, int slot) {
		return handler.getCapacityAsInt(slot, ItemResource.EMPTY);
	}

	/**
	 * How much of {@code stack} the slot could hold.
	 */
	public static int getSlotLimit(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
		return handler.getCapacityAsInt(slot, ItemResource.of(stack));
	}

	public static boolean isItemValid(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
		return handler.isValid(slot, ItemResource.of(stack));
	}

	/**
	 * Insert within a transaction the caller already owns, returning what would not fit.
	 */
	public static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack,
		TransactionContext transaction) {
		return ItemUtil.insertItemReturnRemaining(handler, stack, false, transaction);
	}

	/**
	 * Bridge a handler's ValueIO form to and from a {@link CompoundTag}.
	 * <p>
	 * 26.2 serialises handlers through {@link ValueIOSerializable} rather than the old
	 * {@code serializeNBT}/{@code deserializeNBT} pair. Create's block entities still read and write
	 * CompoundTags, so these keep the stored shape identical while the call sites stay as they were.
	 */
	public static CompoundTag serializeNBT(ValueIOSerializable handler, HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		handler.serialize(output);
		return output.buildResult();
	}

	public static void deserializeNBT(ValueIOSerializable handler, HolderLookup.Provider registries, CompoundTag nbt) {
		handler.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt));

		// GAMETEST FIX - a stopgap: what is written this way wants a datafixer, not a read-time fallback.
		// An inventory written before the handler serialized through ValueIO kept its slots in an "Items"
		// list, each naming its own "Slot"; read the new way that is not found and the whole inventory
		// comes back empty, without complaint. Those are still about - in worlds from an older version, in
		// schematics, in the structures the game tests are built from.
		if (nbt.contains("Items") && handler instanceof ResourceHandler<?> handlingResources)
			readLegacyItems(nbt, handlingResources);
	}

	@SuppressWarnings("unchecked")
	private static void readLegacyItems(CompoundTag nbt, ResourceHandler<?> handlingResources) {

		ResourceHandler<ItemResource> handler = (ResourceHandler<ItemResource>) handlingResources;

		for (int i = 0; i < handler.size(); i++)
			if (!getStackInSlot(handler, i).isEmpty())
				return; // already read, whatever is in the list is the same thing said twice

		for (Tag entry : nbt.getListOrEmpty("Items")) {
			if (!(entry instanceof CompoundTag stackTag))
				continue;

			int slot = stackTag.getIntOr("Slot", -1);
			ItemStack stack = legacyStack(stackTag);

			if (slot >= 0 && slot < handler.size() && !stack.isEmpty())
				setStackInSlot(handler, slot, stack);
		}
	}

	/**
	 * A stack from before an item carried components: a name and a count, and nothing else that survives.
	 */
	private static ItemStack legacyStack(CompoundTag stackTag) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(stackTag.getStringOr("id", "minecraft:air")))
			.map(item -> new ItemStack(item, stackTag.getByteOr("Count", (byte) 1)))
			.orElse(ItemStack.EMPTY);
	}

}
