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
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The same, for the reworked movement a game rule turns on.
 * <p>
 * That one walks the track in steps within a single tick, so a rail may hear about the same cart more
 * than once in a tick. Create Fly tells the first step from the rest and only announces that one; the
 * type it reads to do so is not visible here, and neither listener minds being told twice - an
 * assembler notes the cart for next tick, a controller rail sets a speed it would set again.
 * <p>
 * The place to put this was taken from Create Fly (github.com/zurrtum/create-fly), which had ported it
 * already.
 *
 * @see MinecartPassBlock
 */
@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {

	protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
		super(minecart);
	}

	@Inject(method = "moveAlongTrack(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;makeStepAlongTrack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/properties/RailShape;D)D"))
	private void create$onMinecartPass(ServerLevel level, CallbackInfo ci, @Local BlockPos pos,
		@Local BlockState state) {

		if (state.getBlock() instanceof MinecartPassBlock rail)
			rail.onMinecartPass(state, level, pos, minecart);
	}
}
