package com.simibubi.create.api.contraption.storage.item.chest;

import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorageType;

import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ChestMountedStorageType extends SimpleMountedStorageType<ChestMountedStorage> {
	public ChestMountedStorageType() {
		super(ChestMountedStorage.CODEC);
	}

	@Override
	protected ResourceHandler<ItemResource> getHandler(Level level, BlockEntity be) {
		return be instanceof Container container ? VanillaContainerWrapper.of(container) : null;
	}

	@Override
	protected SimpleMountedStorage createStorage(ResourceHandler<ItemResource> handler) {
		return new ChestMountedStorage(handler);
	}
}
