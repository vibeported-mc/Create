package com.simibubi.create.foundation.item;

import com.simibubi.create.foundation.item.ItemStackHandler;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;

import net.createmod.catnip.api.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
public class ItemHelper {

	public static boolean sameItem(ItemStack stack, ItemStack otherStack) {
		return !otherStack.isEmpty() && stack.is(otherStack.getItem());
	}

	public static Predicate<ItemStack> sameItemPredicate(ItemStack stack) {
		return s -> sameItem(stack, s);
	}

	public static void dropContents(Level world, BlockPos pos, ResourceHandler<ItemResource> inv) {
		for (int slot = 0; slot < inv.size(); slot++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), ItemHandlerHelpers.getStackInSlot(inv, slot));
	}

	public static List<ItemStack> multipliedOutput(ItemStack in, ItemStack out) {
		List<ItemStack> stacks = new ArrayList<>();
		ItemStack result = out.copy();
		result.setCount(in.getCount() * out.getCount());

		while (result.getCount() > result.getMaxStackSize()) {
			stacks.add(result.split(result.getMaxStackSize()));
		}

		stacks.add(result);
		return stacks;
	}

	public static void addToList(ItemStack stack, List<ItemStack> stacks) {
		for (ItemStack s : stacks) {
			if (!ItemStack.isSameItemSameComponents(stack, s))
				continue;
			int transferred = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
			s.grow(transferred);
			stack.shrink(transferred);
		}
		if (stack.getCount() > 0)
			stacks.add(stack);
	}

	public static boolean isSameInventory(ResourceHandler<ItemResource> h1, ResourceHandler<ItemResource> h2) {
		if (h1 == null || h2 == null)
			return false;
		if (h1.size() != h2.size())
			return false;
		for (int slot = 0; slot < h1.size(); slot++) {
			if (ItemHandlerHelpers.getStackInSlot(h1, slot) != ItemHandlerHelpers.getStackInSlot(h2, slot))
				return false;
		}
		return true;
	}

	public static <T extends IBE<? extends BlockEntity>> int calcRedstoneFromBlockEntity(T ibe, Level level, BlockPos pos) {
		return ibe.getBlockEntityOptional(level, pos)
			.map(be -> level.getCapability(Capabilities.Item.BLOCK, pos, null))
			.map(ItemHelper::calcRedstoneFromInventory)
			.orElse(0);
	}

	public static int calcRedstoneFromInventory(@Nullable ResourceHandler<ItemResource> inv) {
		if (inv == null)
			return 0;
		int i = 0;
		float f = 0.0F;
		int totalSlots = inv.size();

		for (int j = 0; j < inv.size(); ++j) {
			int slotLimit = ItemHandlerHelpers.getSlotLimit(inv, j);
			if (slotLimit == 0) {
				totalSlots--;
				continue;
			}
			ItemStack itemstack = ItemHandlerHelpers.getStackInSlot(inv, j);
			if (!itemstack.isEmpty()) {
				f += (float) itemstack.getCount() / (float) Math.min(slotLimit, itemstack.getMaxStackSize());
				++i;
			}
		}

		if (totalSlots == 0)
			return 0;

		f = f / totalSlots;
		return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
	}

	public static List<Pair<Ingredient, MutableInt>> condenseIngredients(NonNullList<Ingredient> recipeIngredients) {
		List<Pair<Ingredient, MutableInt>> actualIngredients = new ArrayList<>();
		Ingredients:
		for (Ingredient igd : recipeIngredients) {
			for (Pair<Ingredient, MutableInt> pair : actualIngredients) {
				ItemStack[] stacks1 = getItems(pair.getFirst());
				ItemStack[] stacks2 = getItems(igd);
				if (stacks1.length != stacks2.length)
					continue;
				for (int i = 0; i <= stacks1.length; i++) {
					if (i == stacks1.length) {
						pair.getSecond()
							.increment();
						continue Ingredients;
					}
					if (!ItemStack.matches(stacks1[i], stacks2[i]))
						break;
				}
			}
			actualIngredients.add(Pair.of(igd, new MutableInt(1)));
		}
		return actualIngredients;
	}

	public static boolean matchIngredients(Ingredient i1, Ingredient i2) {
		if (i1 == i2)
			return true;
		ItemStack[] stacks1 = getItems(i1);
		ItemStack[] stacks2 = getItems(i2);
		if (stacks1 == stacks2)
			return true;
		if (stacks1.length == stacks2.length) {
			for (int i = 0; i < stacks1.length; i++)
				if (!ItemStack.isSameItem(stacks1[i], stacks2[i]))
					return false;
			return true;
		}
		return false;
	}

	public static boolean matchAllIngredients(NonNullList<Ingredient> ingredients) {
		if (ingredients.size() <= 1)
			return true;
		Ingredient firstIngredient = ingredients.get(0);
		for (int i = 1; i < ingredients.size(); i++)
			if (!matchIngredients(firstIngredient, ingredients.get(i)))
				return false;
		return true;
	}

	public static enum ExtractionCountMode {
		EXACTLY, UPTO
	}

	public static ItemStack extract(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test, boolean simulate) {
		return extract(inv, test, ExtractionCountMode.UPTO, 64, simulate);
	}

	public static ItemStack extract(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test, int exactAmount, boolean simulate) {
		return extract(inv, test, ExtractionCountMode.EXACTLY, exactAmount, simulate);
	}

	public static ItemStack extract(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test, ExtractionCountMode mode, int amount,
									boolean simulate) {
		ItemStack extracting = ItemStack.EMPTY;
		boolean amountRequired = mode == ExtractionCountMode.EXACTLY;
		boolean checkHasEnoughItems = amountRequired;
		boolean hasEnoughItems = !checkHasEnoughItems;
		boolean potentialOtherMatch = false;
		int maxExtractionCount = amount;

		Extraction:
		do {
			extracting = ItemStack.EMPTY;

			for (int slot = 0; slot < inv.size(); slot++) {
				ItemStack slotStack = ItemHandlerHelpers.getStackInSlot(inv, slot);
				if (slotStack.isEmpty())
					continue;
				int amountToExtractFromThisSlot =
					Math.min(maxExtractionCount - extracting.getCount(), slotStack.getMaxStackSize());
				ItemStack stack = ItemHandlerHelpers.extractItem(inv, slot, amountToExtractFromThisSlot, true);

				if (stack.isEmpty())
					continue;
				if (!test.test(stack))
					continue;
				if (!extracting.isEmpty() && !canItemStackAmountsStack(stack, extracting)) {
					potentialOtherMatch = true;
					continue;
				}

				if (extracting.isEmpty())
					extracting = stack.copy();
				else
					extracting.grow(stack.getCount());

				if (!simulate && hasEnoughItems)
					ItemHandlerHelpers.extractItem(inv, slot, stack.getCount(), false);

				if (extracting.getCount() >= maxExtractionCount) {
					if (checkHasEnoughItems) {
						hasEnoughItems = true;
						checkHasEnoughItems = false;
						continue Extraction;
					} else {
						break Extraction;
					}
				}
			}

			if (!extracting.isEmpty() && !hasEnoughItems && potentialOtherMatch) {
				ItemStack blackListed = extracting.copy();
				test = test.and(i -> !ItemStack.isSameItemSameComponents(i, blackListed));
				continue;
			}

			if (checkHasEnoughItems)
				checkHasEnoughItems = false;
			else
				break Extraction;

		} while (true);

		if (amountRequired && extracting.getCount() < amount)
			return ItemStack.EMPTY;

		return extracting;
	}

	public static ItemStack extract(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test,
									Function<ItemStack, Integer> amountFunction, boolean simulate) {
		ItemStack extracting = ItemStack.EMPTY;
		int maxExtractionCount = 64;

		for (int slot = 0; slot < inv.size(); slot++) {
			if (extracting.isEmpty()) {
				ItemStack stackInSlot = ItemHandlerHelpers.getStackInSlot(inv, slot);
				if (stackInSlot.isEmpty() || !test.test(stackInSlot))
					continue;
				int maxExtractionCountForItem = amountFunction.apply(stackInSlot);
				if (maxExtractionCountForItem == 0)
					continue;
				maxExtractionCount = Math.min(maxExtractionCount, maxExtractionCountForItem);
			}

			ItemStack stack = ItemHandlerHelpers.extractItem(inv, slot, maxExtractionCount - extracting.getCount(), true);

			if (!test.test(stack))
				continue;
			if (!extracting.isEmpty() && !canItemStackAmountsStack(stack, extracting))
				continue;

			if (extracting.isEmpty())
				extracting = stack.copy();
			else
				extracting.grow(stack.getCount());

			if (!simulate)
				ItemHandlerHelpers.extractItem(inv, slot, stack.getCount(), false);
			if (extracting.getCount() >= maxExtractionCount)
				break;
		}

		return extracting;
	}

	public static boolean canItemStackAmountsStack(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b) && a.getCount() + b.getCount() <= a.getMaxStackSize();
	}

	public static ItemStack findFirstMatch(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test) {
		int slot = findFirstMatchingSlotIndex(inv, test);
		if (slot == -1)
			return ItemStack.EMPTY;
		else
			return ItemHandlerHelpers.getStackInSlot(inv, slot);
	}

	public static int findFirstMatchingSlotIndex(ResourceHandler<ItemResource> inv, Predicate<ItemStack> test) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack toTest = ItemHandlerHelpers.getStackInSlot(inv, slot);
			if (test.test(toTest))
				return slot;
		}
		return -1;
	}

	public static ItemStack fromItemEntity(Entity entityIn) {
		if (!entityIn.isAlive())
			return ItemStack.EMPTY;
		if (entityIn instanceof PackageEntity packageEntity) {
			return packageEntity.getBox();
		}
		return entityIn instanceof ItemEntity itemEntity ? itemEntity.getItem() : ItemStack.EMPTY;
	}

	public static void fillItemStackHandler(ItemContainerContents contents, ItemStackHandler inv) {
		List<ItemStack> itemStacks = contents.allItemsCopyStream()
			.toList();

		for (int i = 0; i < itemStacks.size(); i++) {
			ItemHandlerHelpers.setStackInSlot(inv, i, itemStacks.get(i));
		}
	}

	public static ItemContainerContents containerContentsFromHandler(ItemStackHandler handler) {
		return ItemContainerContents.fromItems(((ItemStackHandlerAccessor) handler).create$getStacks());
	}

	public static ItemStack limitCountToMaxStackSize(ItemStack stack, boolean simulate) {
		int count = stack.getCount();
		int max = stack.getMaxStackSize();
		if (count <= max)
			return ItemStack.EMPTY;
		ItemStack remainder = stack.copyWithCount(count - max);
		if (!simulate)
			stack.setCount(max);
		return remainder;
	}

	public static void copyContents(ResourceHandler<ItemResource> from, ModifiableItemHandler to) {
		if (from.size() != to.size()) {
			throw new IllegalArgumentException("Slot count mismatch");
		}

		for (int slot = to.size() - 1; slot >= 0; slot--) {
			ItemHandlerHelpers.setStackInSlot(to, slot, ItemStack.EMPTY);
		}

		for (int i = 0; i < from.size(); i++) {
			ItemHandlerHelpers.setStackInSlot(to, i, ItemHandlerHelpers.getStackInSlot(from, i).copy());
		}
	}

	public static List<ItemStack> getNonEmptyStacks(ItemStackHandler handler) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < handler.size(); i++) {
			ItemStack stack = ItemHandlerHelpers.getStackInSlot(handler, i);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		return stacks;
	}
	/**
	 * 26.2 dropped ItemStack's saveOptional/parseOptional pair in favour of its optional codec.
	 * These keep the stored shape - an empty compound for an empty stack - identical.
	 */
	public static Tag saveOptional(ItemStack stack, HolderLookup.Provider registries) {
		return CatnipCodecUtils.encode(ItemStack.OPTIONAL_CODEC, registries, stack)
			.orElseGet(CompoundTag::new);
	}

	public static ItemStack parseOptional(HolderLookup.Provider registries, Tag tag) {
		return CatnipCodecUtils.decode(ItemStack.OPTIONAL_CODEC, registries, tag)
			.orElse(ItemStack.EMPTY);
	}

	/**
	 * The stack a recipe leaves behind when this one is consumed - an empty bucket for a filled one,
	 * and so on.
	 * <p>
	 * 26.2 moved this off ItemStack: it is a template on the Item now, so it has to be instantiated.
	 */
	/**
	 * One stack per item an ingredient accepts.
	 * <p>
	 * 26.2 replaced Ingredient#getItems with a stream of the item holders it matches.
	 */
	public static ItemStack[] getItems(Ingredient ingredient) {
		return ingredient.items()
			.map(holder -> new ItemStack(holder.value()))
			.toArray(ItemStack[]::new);
	}

	public static ItemStack getCraftingRemainder(ItemStack stack) {
		ItemStackTemplate remainder = stack.getItem()
			.getCraftingRemainder(stack);
		return remainder == null ? ItemStack.EMPTY : remainder.create();
	}

	public static boolean hasCraftingRemainder(ItemStack stack) {
		return stack.getItem()
			.getCraftingRemainder(stack) != null;
	}

}
