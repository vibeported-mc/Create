package com.simibubi.create.content.contraptions.behaviour.dispenser.storage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorageType;

public class DispenserMountedStorageType extends SimpleMountedStorageType<DispenserMountedStorage> {
	public DispenserMountedStorageType() {
		super(DispenserMountedStorage.CODEC);
	}

	@Override
	protected SimpleMountedStorage createStorage(ResourceHandler<ItemResource> handler) {
		return new DispenserMountedStorage(handler);
	}
}
