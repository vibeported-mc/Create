package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.block.MinecartPassBlock;
import com.simibubi.create.foundation.mixin.accessor.MinecartBehaviorAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hands a cart moving along a track back to the rail it is riding.
 * <p>
 * A cart no longer moves itself: 26.2 gave that to a behaviour beside it, of which there are two - the
 * long-standing one and the reworked one a game rule turns on - and left the method on the cart where
 * nothing calls it. Riding the dead one meant a cart assembler was never told a cart had arrived, so it
 * never assembled anything, and a controller rail never set a speed. Both behaviours are ridden here.
 *
 * @see MinecartPassBlock
 */
@Mixin({ OldMinecartBehavior.class, NewMinecartBehavior.class })
public abstract class AbstractMinecartMixin {

	@Inject(method = "moveAlongTrack", at = @At("RETURN"))
	private void create$onMoveAlongTrack(ServerLevel level, CallbackInfo ci) {
		AbstractMinecart cart = ((MinecartBehaviorAccessor) this).create$getMinecart();
		BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof MinecartPassBlock rail)
			rail.onMinecartPass(state, level, pos, cart);
	}

}
