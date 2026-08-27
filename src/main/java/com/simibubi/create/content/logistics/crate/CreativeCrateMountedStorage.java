package com.simibubi.create.content.logistics.crate;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeCrateMountedStorage extends MountedItemStorage {
	public static final MapCodec<CreativeCrateMountedStorage> CODEC = ItemStack.OPTIONAL_CODEC.xmap(
		CreativeCrateMountedStorage::new, storage -> storage.suppliedStack
	).fieldOf("value");

	private final ItemStack suppliedStack;
	private final ItemStack cachedStackInSlot;

	protected CreativeCrateMountedStorage(MountedItemStorageType<?> type, ItemStack suppliedStack) {
		super(type);
		this.suppliedStack = suppliedStack;
		this.cachedStackInSlot = suppliedStack.copyWithCount(suppliedStack.getMaxStackSize());
	}

	public CreativeCrateMountedStorage(ItemStack suppliedStack) {
		this(AllMountedStorageTypes.CREATIVE_CRATE.get(), suppliedStack);
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		// no need to do anything here, the supplied item can't change while mounted
	}

	@Override
	public int size() {
		return 2; // 0 holds the supplied stack endlessly, 1 is always empty to accept
	}

	@Override
	public ItemResource getResource(int index) {
		return index == 0 ? ItemResource.of(this.cachedStackInSlot) : ItemResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int index) {
		return index == 0 ? this.cachedStackInSlot.getCount() : 0;
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return 64;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return true;
	}

	/**
	 * Anything inserted is voided, so the whole amount is reported as accepted.
	 */
	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return amount;
	}

	/**
	 * Slot 0 supplies its stack endlessly, so nothing is consumed and no transaction state is needed.
	 */
	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		if (index != 0 || this.suppliedStack.isEmpty())
			return 0;
		if (!resource.matches(this.suppliedStack))
			return 0;
		return Math.min(amount, this.suppliedStack.getMaxStackSize());
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
	}

}
