package com.simibubi.create.foundation.utility;

import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;

/**
 * Reading and writing a {@link Component} as a JSON string.
 * <p>
 * 26.2 removed {@code Component.Serializer} in favour of {@link ComponentSerialization#CODEC}.
 * Create stores components as JSON strings in a handful of places - block entity custom names,
 * assembly errors - and this keeps that stored form byte-for-byte the same rather than migrating
 * those tags.
 */
public class ComponentJson {

	public static String toJson(Component component, HolderLookup.Provider registries) {
		return ComponentSerialization.CODEC
			.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), component)
			.getOrThrow()
			.toString();
	}

	public static @Nullable Component fromJson(JsonElement json, HolderLookup.Provider registries) {
		return ComponentSerialization.CODEC
			.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json)
			.result()
			.orElse(null);
	}

	public static @Nullable Component fromJson(String json, HolderLookup.Provider registries) {
		if (json.isEmpty())
			return null;
		return ComponentSerialization.CODEC
			.parse(RegistryOps.create(JsonOps.INSTANCE, registries), GsonHelper.parse(json))
			.result()
			.orElse(null);
	}

	private ComponentJson() {
	}

}
