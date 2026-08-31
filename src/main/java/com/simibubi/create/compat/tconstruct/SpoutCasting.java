package com.simibubi.create.compat.tconstruct;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
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

		if (!handler.isValid(0, FluidResource.of(availableFluid)))
			return 0;

		FluidStack containedFluid = FluidUtil.getStack(handler, 0);
		if (!(containedFluid.isEmpty() || FluidStack.isSameFluidSameComponents(containedFluid, availableFluid)))
			return 0;

		// Do not fill if it would only partially fill the table (unless > 1000mb)
		int amount = availableFluid.getAmount();
		if (amount < 1000) {
			FluidStack oneMore = FluidHelper.copyStackWithAmount(availableFluid, amount + 1);

			try (Transaction transaction = Transaction.openRoot()) {
				// never committed, so this only asks the question
				if (handler.insert(FluidResource.of(oneMore), oneMore.getAmount(), transaction) > amount)
					return 0;
			}
		}

		// Return amount filled into the table/basin
		try (Transaction transaction = Transaction.openRoot()) {
			int transferred = availableFluid.isEmpty() ? 0 : handler.insert(FluidResource.of(availableFluid), availableFluid.getAmount(), transaction);
			if (!simulate)
				transaction.commit();
			return transferred;
		}
	}

	private boolean enabled() {
		return AllConfigs.server().recipes.allowCastingBySpout.get();
	}
}
