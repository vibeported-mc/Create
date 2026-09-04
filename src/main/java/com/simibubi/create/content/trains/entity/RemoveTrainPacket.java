package com.simibubi.create.content.trains.entity;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

public record RemoveTrainPacket(UUID id) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, RemoveTrainPacket> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(RemoveTrainPacket::new, RemoveTrainPacket::id);

	public RemoveTrainPacket(Train train) {
		this(train.id);
	}

	public void handle(Player player) {
		CreateClient.RAILWAYS.trains.remove(this.id);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.REMOVE_TRAIN.getType();
	}
}
