package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.block.MinecartPassBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hands a cart moving along a track back to the rail it is riding.
 *
 * @see MinecartPassBlock
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin {

	@Inject(method = "moveAlongTrack", at = @At("RETURN"))
	private void create$onMoveAlongTrack(ServerLevel level, CallbackInfo ci) {
		AbstractMinecart cart = (AbstractMinecart) (Object) this;
		BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof MinecartPassBlock rail)
			rail.onMinecartPass(state, level, pos, cart);
	}

}
