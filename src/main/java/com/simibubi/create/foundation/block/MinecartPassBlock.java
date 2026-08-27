package com.simibubi.create.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rail block that wants to act on carts riding it.
 * <p>
 * NeoForge used to call {@code onMinecartPass} from the cart's own track movement; 26.2 moved that
 * movement into {@code MinecartBehavior} and dropped the hook. Create answers it from
 * {@code AbstractMinecartMixin} instead, at the same point in the tick.
 */
public interface MinecartPassBlock {

	void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart);

}
