package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.block.MinecartPassBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hands a cart moving along a track back to the rail it is riding.
 * <p>
 * A cart no longer moves itself; 26.2 gave that to a behaviour beside it, and left the method on the
 * cart where nothing calls it. This is the long-standing behaviour of the two - the rail is read where
 * the cart itself reads it, from what the move has already worked out.
 * <p>
 * The place to put this was taken from Create Fly (github.com/zurrtum/create-fly), which had ported it
 * already.
 *
 * @see MinecartPassBlock
 */
@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehavior {

	protected OldMinecartBehaviorMixin(AbstractMinecart minecart) {
		super(minecart);
	}

	@Inject(method = "moveAlongTrack(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior;setDeltaMovement(DDD)V", ordinal = 0, shift = At.Shift.BY, by = 2))
	private void create$onMinecartPass(ServerLevel level, CallbackInfo ci, @Local BlockPos pos,
		@Local BlockState state) {

		if (state.getBlock() instanceof MinecartPassBlock rail)
			rail.onMinecartPass(state, level, pos, minecart);
	}
}
