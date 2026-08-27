package com.simibubi.create.foundation.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.events.CommonEvents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public enum LeftClickPacket implements SelfHandlingPayload {
	INSTANCE;

	public static final StreamCodec<ByteBuf, LeftClickPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.LEFT_CLICK.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		CommonEvents.leftClickEmpty(player);
	}
}
