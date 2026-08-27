package com.simibubi.create.content.equipment.tool;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		if (player != null)
			CardboardSwordItem.knockback(player, strength, yRot);
	}
}
