package com.simibubi.create.content.equipment.tool;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KnockbackPacket(float yRot, float strength) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, KnockbackPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, KnockbackPacket::yRot,
	    ByteBufCodecs.FLOAT, KnockbackPacket::strength,
	    KnockbackPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.KNOCKBACK.getType();
	}

	public void handle(Player player) {
		if (player != null)
			CardboardSwordItem.knockback(player, strength, yRot);
	}
}
