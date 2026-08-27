package com.simibubi.create.content.contraptions.gantry;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GantryContraptionUpdatePacket(int entityID, double coord, double motion, double sequenceLimit) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, GantryContraptionUpdatePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, GantryContraptionUpdatePacket::entityID,
			ByteBufCodecs.DOUBLE, GantryContraptionUpdatePacket::coord,
			ByteBufCodecs.DOUBLE, GantryContraptionUpdatePacket::motion,
			ByteBufCodecs.DOUBLE, GantryContraptionUpdatePacket::sequenceLimit,
			GantryContraptionUpdatePacket::new
	);

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		GantryContraptionEntity.handlePacket(this);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.GANTRY_UPDATE.getType();
	}
}
