package com.simibubi.create.foundation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An item that decides whether the player may break the block they are punching.
 * <p>
 * Minecraft 26.2 dropped {@code Item#canAttackBlock}; the decision is made by cancelling
 * NeoForge's {@code BreakBlockEvent} instead. Create's items that used the old hook implement this
 * and {@code CommonEvents} answers the event with it, so the logic stays on the item.
 */
public interface BlockBreakingItem {

	boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player);

}
