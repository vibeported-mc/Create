package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
public class VersionedInventoryTrackerBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<VersionedInventoryTrackerBehaviour> TYPE = new BehaviourType<>();

	private int ignoredId;
	private int ignoredVersion;

	public VersionedInventoryTrackerBehaviour(SmartBlockEntity be) {
		super(be);
		reset();
	}

	public boolean stillWaiting(InvManipulationBehaviour behaviour) {
		return behaviour.hasInventory() && stillWaiting(behaviour.getInventory());
	}

	public boolean stillWaiting(ResourceHandler<ItemResource> handler) {
		if (handler instanceof VersionedInventoryWrapper viw)
			return viw.getId() == ignoredId && viw.getVersion() == ignoredVersion;
		return false;
	}

	public void awaitNewVersion(InvManipulationBehaviour behaviour) {
		if (behaviour.hasInventory())
			awaitNewVersion(behaviour.getInventory());
	}

	public void awaitNewVersion(ResourceHandler<ItemResource> handler) {
		if (handler instanceof VersionedInventoryWrapper viw) {
			ignoredId = viw.getId();
			ignoredVersion = viw.getVersion();
		}
	}

	public void reset() {
		ignoredVersion = -1;
		ignoredId = -1;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}
