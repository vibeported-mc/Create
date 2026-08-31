package com.simibubi.create.content.logistics.redstoneRequester;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RedstoneRequesterEffectPacket(BlockPos pos, boolean success) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, RedstoneRequesterEffectPacket> STREAM_CODEC = StreamCodec.composite(
	    BlockPos.STREAM_CODEC, RedstoneRequesterEffectPacket::pos,
		ByteBufCodecs.BOOL, RedstoneRequesterEffectPacket::success,
	    RedstoneRequesterEffectPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.REDSTONE_REQUESTER_EFFECT.getType();
	}

	public void handle(LocalPlayer player) {
		if (Minecraft.getInstance().level.getBlockEntity(pos) instanceof RedstoneRequesterBlockEntity plbe)
			plbe.playEffect(success);
	}
}
