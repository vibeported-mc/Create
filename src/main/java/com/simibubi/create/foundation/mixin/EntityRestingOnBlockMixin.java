package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.block.EntityRestingOnBlock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tell a block what is moving on it, the way moving used to.
 * <p>
 * See {@link EntityRestingOnBlock} for why Create needs this back. Rather than work out again which
 * block that is, this reads the one the move is already holding - what the game calls the state the
 * entity is being affected by - at the moment it decides whether the entity moves at all.
 * <p>
 * The place to put this was taken from Create Fly (github.com/zurrtum/create-fly), which had ported it
 * already, and where the same hook is called {@code EntityControlBlock.onEntityMovement}.
 */
@Mixin(Entity.class)
public class EntityRestingOnBlockMixin {

	@WrapOperation(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canSimulateMovement()Z"))
	private boolean create$updateEntityAfterFallOn(Entity entity, Operation<Boolean> original,
		@Local BlockState restingOn) {

		if (!original.call(entity))
			return false;

		if (restingOn.getBlock() instanceof EntityRestingOnBlock block)
			block.updateEntityAfterFallOn(entity.level(), entity);

		return true;
	}
}
