package com.simibubi.create.content.equipment.bell;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SoulPulseEffectPacket(BlockPos pos, int distance, boolean canOverlap) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, SoulPulseEffectPacket> STREAM_CODEC = StreamCodec.composite(
	        BlockPos.STREAM_CODEC, SoulPulseEffectPacket::pos,
			ByteBufCodecs.INT, SoulPulseEffectPacket::distance,
			ByteBufCodecs.BOOL, SoulPulseEffectPacket::canOverlap,
	        SoulPulseEffectPacket::new
	);

	public void handle(Player player) {
		CreateClient.SOUL_PULSE_EFFECT_HANDLER.addPulse(new SoulPulseEffect(pos, distance, canOverlap));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SOUL_PULSE.getType();
	}
}
