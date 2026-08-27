package com.simibubi.create.content.kinetics.gauge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class GaugeObservedPacket extends BlockEntityConfigurationPacket<StressGaugeBlockEntity> {
	public static final StreamCodec<ByteBuf, GaugeObservedPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
			GaugeObservedPacket::new, packet -> packet.pos
	);

	public GaugeObservedPacket(BlockPos pos) {
		super(pos);
	}

	@Override
	protected void applySettings(ServerPlayer player, StressGaugeBlockEntity be) {
		be.onObserved();
	}

	@Override
	protected boolean causeUpdate() {
		return false;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.OBSERVER_STRESSOMETER.getType();
	}
}
