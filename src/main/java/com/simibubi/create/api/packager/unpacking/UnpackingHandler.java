package com.simibubi.create.api.packager.unpacking;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.List;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.DefaultUnpackingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for custom handling of box unpacking into storage.
 * <p>
 * This interface is <strong>experimental</strong> as it is for a new feature. It may be revised or relocated,
 * but will likely not change very much.
 */
@Experimental
public interface UnpackingHandler {
	SimpleRegistry<Block, UnpackingHandler> REGISTRY = SimpleRegistry.create();

	/**
	 * The default unpacking handler, simply inserting all items into storage.
	 */
	UnpackingHandler DEFAULT = DefaultUnpackingHandler.INSTANCE;

	/**
	 * Unpack the given items into storage.
	 *
	 * @param items    the list of non-empty item stacks to unpack. May be freely modified
	 * @param orderContext    the order context, if present
	 * @param simulate true if the unpacking should only be simulated
	 * @param parent the transaction this unpacking is part of, or null to start one of its own.
	 *                    A packager is often asked to take a box from inside a transfer that is already
	 *                    under way - a chute or a hopper inserting it - and work done there has to join
	 *                    that transaction rather than open one of its own, which would throw.
	 * @return true if all items have been unpacked successfully
	 */
	boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
		@Nullable PackageOrderWithCrafts orderContext, boolean simulate, @Nullable TransactionContext parent);
}
