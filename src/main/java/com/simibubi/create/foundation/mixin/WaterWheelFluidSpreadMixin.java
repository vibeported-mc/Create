package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelStructuralBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Water sitting on a water wheel runs along the wheel rather than off its sides.
 * <p>
 * Fluid used to ask {@code canPassThrough} both when picking which way to run and when actually going
 * that way, so refusing there was enough. It now asks {@code canMaybePassThrough} to decide whether a
 * side is open at all, and {@code canPassThrough} only through the search for the nearest way down - so
 * refusing in the old place alone lets the water off the side of a wheel while still telling the search
 * it cannot go there. Both are answered.
 * <p>
 * The game tests {@code large_waterwheel} and {@code small_waterwheel} watch for this: they pour water
 * onto the top of a wheel and assert the two blocks beside its faces never hold any. Take either
 * injection away and both fail, on this version and on the one before it, with water at the block
 * over the wheel's shoulder.
 */
@Mixin(FlowingFluid.class)
public class WaterWheelFluidSpreadMixin {
	@Inject(method = "canPassThrough(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true)
	protected void create$canPassThroughOnWaterWheel(BlockGetter pLevel, Fluid pFluid, BlockPos pFromPos, BlockState p_75967_,
		Direction pDirection, BlockPos p_75969_, BlockState p_75970_, FluidState p_75971_,
		CallbackInfoReturnable<Boolean> cir) {

		create$refuseAcrossAWaterWheel(pLevel, pFromPos, pDirection, cir);
	}

	@Inject(method = "canMaybePassThrough(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true)
	protected void create$canMaybePassThroughOnWaterWheel(BlockGetter pLevel, BlockPos pFromPos, BlockState p_75967_,
		Direction pDirection, BlockPos p_75969_, BlockState p_75970_, FluidState p_75971_,
		CallbackInfoReturnable<Boolean> cir) {

		create$refuseAcrossAWaterWheel(pLevel, pFromPos, pDirection, cir);
	}

	@Unique
	private static void create$refuseAcrossAWaterWheel(BlockGetter pLevel, BlockPos pFromPos, Direction pDirection,
		CallbackInfoReturnable<Boolean> cir) {

		if (pDirection.getAxis() == Axis.Y)
			return;

		BlockPos belowPos = pFromPos.below();
		BlockState belowState = pLevel.getBlockState(belowPos);

		if (AllBlocks.WATER_WHEEL_STRUCTURAL.has(belowState)) {
			if (AllBlocks.WATER_WHEEL_STRUCTURAL.get()
				.stillValid(pLevel, belowPos, belowState, false))
				belowState = pLevel.getBlockState(WaterWheelStructuralBlock.getMaster(pLevel, belowPos, belowState));
		} else if (!AllBlocks.WATER_WHEEL.has(belowState))
			return;

		if (belowState.getBlock() instanceof IRotate irotate
			&& irotate.getRotationAxis(belowState) == pDirection.getAxis())
			cir.setReturnValue(false);
	}
}
