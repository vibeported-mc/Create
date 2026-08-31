package com.simibubi.create.content.logistics.packagerLink;

import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record WiFiEffectPacket(BlockPos pos) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, WiFiEffectPacket> STREAM_CODEC = BlockPos.STREAM_CODEC
		.map(WiFiEffectPacket::new, WiFiEffectPacket::pos);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PACKAGER_LINK_EFFECT.getType();
	}

	public void handle(LocalPlayer player) {
		BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(pos);
			if (blockEntity instanceof PackagerLinkBlockEntity plbe)
				plbe.playEffect();
			if (blockEntity instanceof StockTickerBlockEntity plbe)
				plbe.playEffect();
	}

	public static void send(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel)
			NetworkHelper.INSTANCE.sendToClientsAround(serverLevel, pos, 32, new WiFiEffectPacket(pos));
	}
}
