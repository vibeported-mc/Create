package com.simibubi.create.impl.contraption.storage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
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
	protected Optional<ModifiableItemHandler> validate(ResourceHandler<ItemResource> handler) {
		return super.validate(handler).filter(FallbackMountedStorage::isValid);
	}

	public static boolean isValid(ResourceHandler<ItemResource> handler) {
		return handler.getClass() == ItemStacksResourceHandler.class;
	}
}
