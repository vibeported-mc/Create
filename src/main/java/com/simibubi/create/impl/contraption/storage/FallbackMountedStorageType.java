package com.simibubi.create.impl.contraption.storage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorageType;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FallbackMountedStorageType extends SimpleMountedStorageType<FallbackMountedStorage> {
	public FallbackMountedStorageType() {
		super(FallbackMountedStorage.CODEC);
	}

	@Override
	protected ResourceHandler<ItemResource> getHandler(Level level, BlockEntity be) {
		ResourceHandler<ItemResource> handler = super.getHandler(level, be);
		return handler != null && FallbackMountedStorage.isValid(handler) ? handler : null;
	}
}
