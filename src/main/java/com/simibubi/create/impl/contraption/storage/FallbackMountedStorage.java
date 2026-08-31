package com.simibubi.create.impl.contraption.storage;

import com.simibubi.create.foundation.item.ContainerItemHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;

/**
 * A fallback mounted storage impl that will try to be used when no type is
 * registered for a block. This requires that the mounted block provide an item handler
 * whose class is exactly {@link ItemStacksResourceHandler}.
 */
public class FallbackMountedStorage extends SimpleMountedStorage {
	public static final MapCodec<FallbackMountedStorage> CODEC = SimpleMountedStorage.codec(FallbackMountedStorage::new);

	public FallbackMountedStorage(ResourceHandler<ItemResource> handler) {
		super(AllMountedStorageTypes.FALLBACK.get(), handler);
	}

	@Override
	protected Optional<IndexModifier<ItemResource>> validate(ResourceHandler<ItemResource> handler) {
		return super.validate(handler)
			.filter(FallbackMountedStorage::isValid);
	}

	public static boolean isValid(IndexModifier<?> handler) {
		return handler.getClass() == ContainerItemHandler.class;
	}
}
