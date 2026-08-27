package com.simibubi.create.content.logistics.stockTicker;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class LogisticalStockRequestPacket extends BlockEntityConfigurationPacket<StockCheckingBlockEntity> {
	public static final StreamCodec<ByteBuf, LogisticalStockRequestPacket> STREAM_CODEC = BlockPos.STREAM_CODEC
		.map(LogisticalStockRequestPacket::new, packet -> packet.pos);

	public LogisticalStockRequestPacket(BlockPos pos) {
		super(pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.LOGISTICS_STOCK_REQUEST.getType();
	}

	@Override
	protected void applySettings(ServerPlayer player, StockCheckingBlockEntity be) {
		be.getRecentSummary()
			.divideAndSendTo(player, pos);
	}

	@Override
	protected int maxRange() {
		return 4096;
	}
}
