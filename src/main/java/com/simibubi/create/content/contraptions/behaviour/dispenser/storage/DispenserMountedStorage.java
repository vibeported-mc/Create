package com.simibubi.create.content.contraptions.behaviour.dispenser.storage;

import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.menu.MountedStorageMenus;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DispenserMountedStorage extends SimpleMountedStorage {
	public static final MapCodec<DispenserMountedStorage> CODEC = SimpleMountedStorage.codec(DispenserMountedStorage::new);

	protected DispenserMountedStorage(MountedItemStorageType<?> type, ResourceHandler<ItemResource> handler) {
		super(type, handler);
	}

	public DispenserMountedStorage(ResourceHandler<ItemResource> handler) {
		this(AllMountedStorageTypes.DISPENSER.get(), handler);
	}

	@Override
	@Nullable
	protected MenuProvider createMenuProvider(Component name, ResourceHandler<ItemResource> handler,
		IndexModifier<ItemResource> writable,
											  Predicate<Player> stillValid, Consumer<Player> onClose) {
		return MountedStorageMenus.createGeneric9x9(name, handler, writable, stillValid, onClose);
	}

	@Override
	protected void playOpeningSound(ServerLevel level, Vec3 pos) {
		// dispensers are silent
	}
}
