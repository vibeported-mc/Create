package com.simibubi.create.content.contraptions;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.AllPackets;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

public record MountedStorageSyncPacket(int contraptionId, Map<BlockPos, MountedItemStorage> items, Map<BlockPos, MountedFluidStorage> fluids) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageSyncPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, MountedStorageSyncPacket::contraptionId,
	    ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedItemStorage.STREAM_CODEC), MountedStorageSyncPacket::items,
	    ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedFluidStorage.STREAM_CODEC), MountedStorageSyncPacket::fluids,
	    MountedStorageSyncPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.MOUNTED_STORAGE_SYNC.getType();
	}

	public void handle(LocalPlayer player) {
		Entity entity = Minecraft.getInstance().level.getEntity(this.contraptionId);
		if (!(entity instanceof AbstractContraptionEntity contraption))
			return;

		contraption.getContraption().getStorage().handleSync(this, contraption);
	}
}
