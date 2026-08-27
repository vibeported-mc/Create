package com.simibubi.create.compat.trainmap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class TrainMapSyncRequestPacket implements SelfHandlingPayload {
	public static final TrainMapSyncRequestPacket INSTANCE = new TrainMapSyncRequestPacket();
	public static final StreamCodec<ByteBuf, TrainMapSyncRequestPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public void handle(ServerPlayer player) {
		TrainMapSync.requestReceived(player);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.TRAIN_MAP_REQUEST.getType();
	}
}
