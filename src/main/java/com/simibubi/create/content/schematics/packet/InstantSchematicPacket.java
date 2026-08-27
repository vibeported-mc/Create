package com.simibubi.create.content.schematics.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record InstantSchematicPacket(String name, BlockPos origin, BlockPos bounds) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, InstantSchematicPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, InstantSchematicPacket::name,
			BlockPos.STREAM_CODEC, InstantSchematicPacket::origin,
			BlockPos.STREAM_CODEC, InstantSchematicPacket::bounds,
	        InstantSchematicPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.INSTANT_SCHEMATIC.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		Create.SCHEMATIC_RECEIVER.handleInstantSchematic(player, name, player.level(), origin, bounds);
	}
}
