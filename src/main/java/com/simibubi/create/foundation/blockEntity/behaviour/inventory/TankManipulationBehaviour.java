package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import com.simibubi.create.foundation.fluid.FluidHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.util.function.Predicate;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import com.google.common.base.Predicates;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

public class TankManipulationBehaviour extends CapManipulationBehaviourBase<ResourceHandler<FluidResource>, TankManipulationBehaviour> {

	public static final BehaviourType<TankManipulationBehaviour> OBSERVE = new BehaviourType<>();
	private BehaviourType<TankManipulationBehaviour> behaviourType;

	public TankManipulationBehaviour(SmartBlockEntity be, InterfaceProvider target) {
		this(OBSERVE, be, target);
	}

	private TankManipulationBehaviour(BehaviourType<TankManipulationBehaviour> type, SmartBlockEntity be,
		InterfaceProvider target) {
		super(be, target);
		behaviourType = type;
	}

	public FluidStack extractAny() {
		if (!hasInventory())
			return FluidStack.EMPTY;
		ResourceHandler<FluidResource> inventory = getInventory();
		Predicate<FluidStack> filterTest = getFilterTest(Predicates.alwaysTrue());
		for (int i = 0; i < inventory.size(); i++) {
			FluidStack fluidInTank = FluidHandlerHelpers.getFluidInTank(inventory, i);
			if (fluidInTank.isEmpty())
				continue;
			if (!filterTest.test(fluidInTank))
				continue;
			FluidStack drained =
				FluidHandlerHelpers.drain(inventory, fluidInTank, simulateNext);
			if (!drained.isEmpty())
				return drained;
		}

		return FluidStack.EMPTY;
	}

	protected Predicate<FluidStack> getFilterTest(Predicate<FluidStack> customFilter) {
		Predicate<FluidStack> test = customFilter;
		FilteringBehaviour filter = blockEntity.getBehaviour(FilteringBehaviour.TYPE);
		if (filter != null)
			test = customFilter.and(filter::test);
		return test;
	}

	@Override
	protected BlockCapability<ResourceHandler<FluidResource>, Direction> capability() {
		return Capabilities.Fluid.BLOCK;
	}

	@Override
	public BehaviourType<?> getType() {
		return behaviourType;
	}

}
