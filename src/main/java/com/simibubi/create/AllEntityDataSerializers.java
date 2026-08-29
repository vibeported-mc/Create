package com.simibubi.create;

import com.simibubi.create.content.trains.entity.CarriageSyncDataSerializer;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.jetbrains.annotations.ApiStatus.Internal;

public class AllEntityDataSerializers {
	private static final DeferredRegister<EntityDataSerializer<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Create.ID);

	public static final CarriageSyncDataSerializer CARRIAGE_DATA = new CarriageSyncDataSerializer();

	public static final DeferredHolder<EntityDataSerializer<?>, CarriageSyncDataSerializer> CARRIAGE_DATA_ENTRY = REGISTER.register("carriage_data", () -> CARRIAGE_DATA);

	/**
	 * 26.2 dropped EntityDataSerializers.OPTIONAL_UUID, and a serializer has to be registered before an
	 * entity is allowed to define data with it - building one on the spot fails the moment the entity
	 * is constructed. Contraptions track the player controlling them, a minecart coupling and a track
	 * graph this way, so the replacement lives here where it can be registered once.
	 */
	public static final EntityDataSerializer<Optional<UUID>> OPTIONAL_UUID =
		EntityDataSerializer.forValueType(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC));

	public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<UUID>>> OPTIONAL_UUID_ENTRY =
		REGISTER.register("optional_uuid", () -> OPTIONAL_UUID);

	@Internal
	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
