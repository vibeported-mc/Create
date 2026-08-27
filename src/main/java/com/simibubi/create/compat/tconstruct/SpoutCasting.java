package com.simibubi.create.compat.tconstruct;

import com.simibubi.create.foundation.fluid.FluidHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
public enum SpoutCasting implements BlockSpoutingBehaviour {
	INSTANCE;

	@Override
	public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
		if (!enabled())
			return 0;

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null)
			return 0;

		ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, blockEntity.getBlockPos(), Direction.UP);
		if (handler == null)
			return 0;
		if (handler.size() != 1)
			return 0;

		if (!FluidHandlerHelpers.isFluidValid(handler, 0, availableFluid))
			return 0;

		FluidStack containedFluid = FluidHandlerHelpers.getFluidInTank(handler, 0);
		if (!(containedFluid.isEmpty() || FluidStack.isSameFluidSameComponents(containedFluid, availableFluid)))
			return 0;

		// Do not fill if it would only partially fill the table (unless > 1000mb)
		int amount = availableFluid.getAmount();
		if (amount < 1000
			&& FluidHandlerHelpers.fill(handler, FluidHelper.copyStackWithAmount(availableFluid, amount + 1), true) > amount)
			return 0;

		// Return amount filled into the table/basin
		return handler.fill(availableFluid, simulate);
	}

	private boolean enabled() {
		return AllConfigs.server().recipes.allowCastingBySpout.get();
	}
}
