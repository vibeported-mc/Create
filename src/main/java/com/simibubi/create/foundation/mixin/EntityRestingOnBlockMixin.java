package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.block.EntityRestingOnBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Tell a block what has come to rest on it, the way moving used to.
 * <p>
 * See {@link EntityRestingOnBlock} for why Create needs this back. The place is the one the game
 * itself used: straight after a move has worked out its fall, while it still knows the move was cut
 * short from below.
 */
@Mixin(Entity.class)
public class EntityRestingOnBlockMixin {

	@Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"))
	private void create$tellWhatIsUnderneath(MoverType type, Vec3 movement, CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;

		if (entity.isRemoved() || !entity.verticalCollision)
			return;

		BlockPos restingOn = entity.getOnPosLegacy();

		if (entity.level()
			.getBlockState(restingOn)
			.getBlock() instanceof EntityRestingOnBlock block)
			block.updateEntityAfterFallOn(entity.level(), entity);
	}
}
