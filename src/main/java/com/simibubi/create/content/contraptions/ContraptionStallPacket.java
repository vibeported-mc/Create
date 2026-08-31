package com.simibubi.create.content.contraptions;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ContraptionStallPacket(int entityId, double x, double y, double z, float angle) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ContraptionStallPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, ContraptionStallPacket::entityId,
			ByteBufCodecs.DOUBLE, ContraptionStallPacket::x,
			ByteBufCodecs.DOUBLE, ContraptionStallPacket::y,
			ByteBufCodecs.DOUBLE, ContraptionStallPacket::z,
			ByteBufCodecs.FLOAT, ContraptionStallPacket::angle,
			ContraptionStallPacket::new
	);

	public void handle(LocalPlayer player) {
		AbstractContraptionEntity.handleStallPacket(this);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_STALL.getType();
	}
}
