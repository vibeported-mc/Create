package com.simibubi.create.content.equipment.toolbox;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemSlots;

import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

// TODO - This should use NonNullList<ItemStack>
public class ToolboxInventory extends ItemStacksResourceHandler {
	public static final int STACKS_PER_COMPARTMENT = 4;
	public static final Codec<ToolboxInventory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ItemSlots.maxSizeCodec(8 * STACKS_PER_COMPARTMENT).fieldOf("items").forGetter(ItemSlots::fromHandler),
		ItemStack.OPTIONAL_CODEC.listOf().fieldOf("filters").forGetter(toolbox -> toolbox.filters)
	).apply(instance, ToolboxInventory::deserialize));

	public static final StreamCodec<RegistryFriendlyByteBuf, ToolboxInventory> STREAM_CODEC = StreamCodec.composite(
		ItemSlots.STREAM_CODEC, ItemSlots::fromHandler,
		CatnipStreamCodecBuilders.list(ItemStack.OPTIONAL_STREAM_CODEC), toolbox -> toolbox.filters,
		ToolboxInventory::deserialize
	);

	@ScheduledForRemoval(inVersion = "1.21.1+ Port")
	@Deprecated(since = "6.0.6", forRemoval = true)
	public static final Codec<ToolboxInventory> BACKWARDS_COMPAT_CODEC = Codec.withAlternative(
		CODEC,
		ItemContainerContents.CODEC.xmap(i -> {
			ToolboxInventory inv = new ToolboxInventory(null);
			ItemHelper.fillItemStackHandler(i, inv);
			return inv;
		}, ItemHelper::containerContentsFromHandler)
	);

	List<ItemStack> filters;
	boolean settling;
	private final ToolboxBlockEntity blockEntity;

	private boolean limitedMode;

	public ToolboxInventory(ToolboxBlockEntity be) {
		super(8 * STACKS_PER_COMPARTMENT);
		this.blockEntity = be;
		limitedMode = false;
		filters = new ArrayList<>();
		settling = false;
		for (int i = 0; i < 8; i++)
			filters.add(ItemStack.EMPTY);
	}

	public void inLimitedMode(Consumer<ToolboxInventory> action) {
		limitedMode = true;
		action.accept(this);
		limitedMode = false;
	}

	public void settle(int compartment) {
		int totalCount = 0;
		boolean valid = true;
		boolean shouldBeEmpty = false;
		ItemStack sample = ItemStack.EMPTY;

		for (int i = 0; i < STACKS_PER_COMPARTMENT; i++) {
			ItemStack stackInSlot = ItemUtil.getStack(this, compartment * STACKS_PER_COMPARTMENT + i);
			totalCount += stackInSlot.getCount();
			if (!shouldBeEmpty)
				shouldBeEmpty = stackInSlot.isEmpty() || stackInSlot.getCount() != stackInSlot.getMaxStackSize();
			else if (!stackInSlot.isEmpty()) {
				valid = false;
				sample = stackInSlot;
			}
		}

		if (valid)
			return;

		settling = true;
		if (!sample.isStackable()) {
			for (int i = 0; i < STACKS_PER_COMPARTMENT; i++) {
				if (!ItemUtil.getStack(this, compartment * STACKS_PER_COMPARTMENT + i).isEmpty())
					continue;
				for (int j = i + 1; j < STACKS_PER_COMPARTMENT; j++) {
					ItemStack stackInSlot = ItemUtil.getStack(this, compartment * STACKS_PER_COMPARTMENT + j);
					if (stackInSlot.isEmpty())
						continue;
					this.set(compartment * STACKS_PER_COMPARTMENT + i, ItemResource.of(stackInSlot), stackInSlot.getCount());
					this.set(compartment * STACKS_PER_COMPARTMENT + j, ItemResource.EMPTY, 0);
					break;
				}
			}
		} else {
			for (int i = 0; i < STACKS_PER_COMPARTMENT; i++) {
				ItemStack copy = totalCount <= 0 ? ItemStack.EMPTY
					: sample.copyWithCount(Math.min(totalCount, sample.getMaxStackSize()));
				this.set(compartment * STACKS_PER_COMPARTMENT + i, ItemResource.of(copy), copy.getCount());
				totalCount -= copy.getCount();
			}
		}
		settling = false;
		notifyUpdate();
	}

	@Override
	public boolean isValid(int slot, ItemResource resource) {
		ItemStack stack = resource.toStack(1);
		if (!stack.getItem()
			.canFitInsideContainerItems())
			return false;

		if (slot < 0 || slot >= size())
			return false;
		int compartment = slot / STACKS_PER_COMPARTMENT;
		ItemStack filter = filters.get(compartment);
		if (limitedMode && filter.isEmpty())
			return false;
		if (filter.isEmpty() || ToolboxInventory.canItemsShareCompartment(filter, stack))
			return super.isValid(slot, resource);
		return false;
	}

	@Override
	public void set(int slot, ItemResource resource, int amount) {
		super.set(slot, resource, amount);
		claimCompartment(slot, resource);
	}

	@Override
	public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
		int inserted = super.insert(slot, resource, amount, transaction);
		// An empty compartment takes on the first thing put into it as its filter, but only once the
		// insertion is actually kept.
		if (inserted > 0)
			new RootCommitJournal(() -> claimCompartment(slot, resource)).updateSnapshots(transaction);
		return inserted;
	}

	private void claimCompartment(int slot, ItemResource resource) {
		if (resource.isEmpty())
			return;
		int compartment = slot / STACKS_PER_COMPARTMENT;
		if (!filters.get(compartment)
			.isEmpty())
			return;
		filters.set(compartment, resource.toStack(1));
		notifyUpdate();
	}

	/**
	 * The compartment filters ride along with the stacks. 26.2 serialises handlers through ValueIO,
	 * so this hooks the handler's own serialize/deserialize rather than a CompoundTag pair.
	 */
	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		output.store("Compartments", ItemStack.CODEC.listOf(), filters);
	}

	@Override
	protected void onContentsChanged(int slot, ItemStack previousContents) {
		if (!settling && (blockEntity == null || !blockEntity.getLevel().isClientSide()))
			settle(slot / STACKS_PER_COMPARTMENT);
		notifyUpdate();
		super.onContentsChanged(slot, previousContents);
	}

	@Override
	public void deserialize(ValueInput input) {
		filters = new ArrayList<>(input.read("Compartments", ItemStack.CODEC.listOf())
			.orElse(List.of()));
		if (filters.size() != 8) {
			filters.clear();
			for (int i = 0; i < 8; i++)
				filters.add(ItemStack.EMPTY);
		}
		super.deserialize(input);
	}

	public ItemStack distributeToCompartment(@NotNull ItemStack stack, int compartment, boolean simulate) {
		if (stack.isEmpty())
			return stack;
		if (filters.get(compartment)
			.isEmpty())
			return stack;

		for (int i = STACKS_PER_COMPARTMENT - 1; i >= 0; i--) {
			int slot = compartment * STACKS_PER_COMPARTMENT + i;
			try (Transaction transaction = Transaction.openRoot()) {
				int transferred = stack.isEmpty() ? 0 : this.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
				stack = transferred == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - transferred);
				if (!simulate)
					transaction.commit();
			}
			if (stack.isEmpty())
				return ItemStack.EMPTY;
		}

		return stack;
	}

	public ItemStack takeFromCompartment(int amount, int compartment, boolean simulate) {
		if (amount == 0)
			return ItemStack.EMPTY;

		int remaining = amount;
		ItemStack lastValid = ItemStack.EMPTY;

		for (int i = STACKS_PER_COMPARTMENT - 1; i >= 0; i--) {
			int slot = compartment * STACKS_PER_COMPARTMENT + i;
			ItemStack extracted;
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred2Resource = this.getResource(slot);
				int transferred2 = transferred2Resource.isEmpty() ? 0 : this.extract(slot, transferred2Resource, remaining, transaction);
				extracted = transferred2 <= 0 ? ItemStack.EMPTY : transferred2Resource.toStack(transferred2);
				if (!simulate)
					transaction.commit();
			}
			remaining -= extracted.getCount();
			if (!extracted.isEmpty())
				lastValid = extracted;
			if (remaining == 0)
				return lastValid.copyWithCount(amount);
		}

		if (remaining == amount)
			return ItemStack.EMPTY;

		return lastValid.copyWithCount(amount - remaining);
	}

	public static ItemStack cleanItemNBT(ItemStack stack) {
		if (AllItems.BELT_CONNECTOR.isIn(stack))
			stack.remove(AllDataComponents.BELT_FIRST_SHAFT);
		return stack;
	}

	public static boolean canItemsShareCompartment(ItemStack stack1, ItemStack stack2) {
		if (!stack1.isStackable() && !stack2.isStackable() && stack1.isDamageableItem() && stack2.isDamageableItem())
			return stack1.getItem() == stack2.getItem();
		if (AllItems.BELT_CONNECTOR.isIn(stack1) && AllItems.BELT_CONNECTOR.isIn(stack2))
			return true;
		return ItemStack.isSameItemSameComponents(stack1, stack2);
	}

	private void notifyUpdate() {
		if (blockEntity != null)
			blockEntity.notifyUpdate();
	}

	private static ToolboxInventory deserialize(ItemSlots slots, List<ItemStack> filters) {
		ToolboxInventory inventory = new ToolboxInventory(null);
		inventory.settling = true;
		slots.forEach((slot, stack) -> inventory.set(slot, ItemResource.of(stack), stack.getCount()));
		inventory.settling = false;
		inventory.filters = new ArrayList<>(filters);
		return inventory;
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof ToolboxInventory that)) return false;

		return settling == that.settling && limitedMode == that.limitedMode && ItemStack.listMatches(filters, that.filters)
			&& Objects.equals(blockEntity, that.blockEntity);
	}

	@Override
	public int hashCode() {
		int result = filters.hashCode();
		result = 31 * result + Boolean.hashCode(settling);
		result = 31 * result + Objects.hashCode(blockEntity);
		result = 31 * result + Boolean.hashCode(limitedMode);
		return result;
	}
}
