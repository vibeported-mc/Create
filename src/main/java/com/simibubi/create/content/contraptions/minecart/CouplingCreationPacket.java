package com.simibubi.create.content.contraptions.minecart;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public record CouplingCreationPacket(int id1, int id2) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, CouplingCreationPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CouplingCreationPacket::id1,
			ByteBufCodecs.VAR_INT, CouplingCreationPacket::id2,
			CouplingCreationPacket::new
	);

	public CouplingCreationPacket(AbstractMinecart cart1, AbstractMinecart cart2) {
		this(cart1.getId(), cart2.getId());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.MINECART_COUPLING_CREATION.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		CouplingHandler.tryToCoupleCarts(player, player.level(), id1, id2);
	}
}
