package com.simibubi.create.content.contraptions;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ContraptionRelocationPacket(int entityId) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ContraptionRelocationPacket> STREAM_CODEC = ByteBufCodecs.INT.map(
			ContraptionRelocationPacket::new, ContraptionRelocationPacket::entityId
	);

	public void handle(LocalPlayer player) {
		OrientedContraptionEntity.handleRelocationPacket(this);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_RELOCATION.getType();
	}
}
