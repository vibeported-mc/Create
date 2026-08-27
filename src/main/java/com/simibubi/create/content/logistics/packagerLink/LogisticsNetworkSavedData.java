package com.simibubi.create.content.logistics.packagerLink;

import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.Create;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class LogisticsNetworkSavedData extends SavedData {

	private Map<UUID, LogisticsNetwork> logisticsNetworks = new HashMap<>();

	public static final SavedDataType<LogisticsNetworkSavedData> TYPE = new SavedDataType<>(
		Identifier.parse("create:create_logistics"), level -> new LogisticsNetworkSavedData(),
		level -> Codec.of(
			CompoundTag.CODEC.comap(data -> data.save(new CompoundTag(), level.registryAccess())),
			CompoundTag.CODEC.map(tag -> load(tag, level.registryAccess()))));

	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
		GlobalLogisticsManager logistics = Create.LOGISTICS;
		nbt.put("LogisticsNetworks",
			NBTHelper.writeCompoundList(logistics.logisticsNetworks.values(), network -> network.write(registries)));
		return nbt;
	}

	private static LogisticsNetworkSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
		LogisticsNetworkSavedData sd = new LogisticsNetworkSavedData();
		sd.logisticsNetworks = new HashMap<>();
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("LogisticsNetworks"), c -> {
			LogisticsNetwork network = LogisticsNetwork.read(c, registries);
			sd.logisticsNetworks.put(network.id, network);
		});
		return sd;
	}

	public Map<UUID, LogisticsNetwork> getLogisticsNetworks() {
		return logisticsNetworks;
	}

	private LogisticsNetworkSavedData() {}

	public static LogisticsNetworkSavedData load(MinecraftServer server) {
		return server.overworld()
			.getDataStorage()
			.computeIfAbsent(TYPE);
	}

}
