package com.simibubi.create.content.fluids.tank;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
public class CreativeFluidTankBlockEntity extends FluidTankBlockEntity {

	public CreativeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Fluid.BLOCK,
				AllBlockEntityTypes.CREATIVE_FLUID_TANK.get(),
				(be, context) -> {
					if (be.fluidCapability == null)
						be.refreshCapability();
					return be.fluidCapability;
				}
		);
	}

	@Override
	protected SmartFluidTank createInventory() {
		return new CreativeSmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return false;
	}

	public static class CreativeSmartFluidTank extends SmartFluidTank {
		public static final Codec<CreativeSmartFluidTank> CODEC = RecordCodecBuilder.create(i -> i.group(
			FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(CreativeSmartFluidTank::getFluid),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(CreativeSmartFluidTank::getCapacity)
		).apply(i, (fluid, capacity) -> {
			CreativeSmartFluidTank tank = new CreativeSmartFluidTank(capacity, $ -> {
			});
			tank.setFluid(fluid);
			return tank;
		}));

		public CreativeSmartFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
			super(capacity, updateCallback);
		}

		@Override
		public int getFluidAmount() {
			return getFluid().isEmpty() ? 0 : getCapacity();
		}

		public void setContainedFluid(FluidStack fluidStack) {
			FluidStack contained = fluidStack.copy();
			if (!contained.isEmpty())
				contained.setAmount(getCapacity());
			setFluid(contained);
		}

		/**
		 * Creative tanks swallow anything and never run dry, so neither direction changes state and
		 * neither needs to join the transaction.
		 */
		@Override
		public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			return amount;
		}

		@Override
		public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			return resource.equals(getResource(tank)) ? amount : 0;
		}

	}

}
