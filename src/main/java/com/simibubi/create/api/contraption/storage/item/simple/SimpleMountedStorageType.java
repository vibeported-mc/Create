package com.simibubi.create.api.contraption.storage.item.simple;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ContainerItemHandler;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
public abstract class SimpleMountedStorageType<T extends SimpleMountedStorage> extends MountedItemStorageType<SimpleMountedStorage> {
	protected SimpleMountedStorageType(MapCodec<T> codec) {
		super(codec);
	}

	@Override
	@Nullable
	public SimpleMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		return Optional.ofNullable(be)
			.map(b -> getHandler(level, b))
			.map(this::createStorage)
			.orElse(null);
	}

	protected ResourceHandler<ItemResource> getHandler(Level level, BlockEntity be) {
		ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, be.getBlockPos(), null);
		// make sure the handler is modifiable so new contents can be moved over on disassembly
		return ContainerItemHandler.writable(handler, be);
	}

	protected SimpleMountedStorage createStorage(ResourceHandler<ItemResource> handler) {
		return new SimpleMountedStorage(this, handler);
	}

	public static final class Impl extends SimpleMountedStorageType<SimpleMountedStorage> {
		public Impl() {
			super(SimpleMountedStorage.CODEC);
		}
	}
}
