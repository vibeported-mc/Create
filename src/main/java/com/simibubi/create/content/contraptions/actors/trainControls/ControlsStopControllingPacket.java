package com.simibubi.create.content.contraptions.actors.trainControls;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.codec.StreamCodec;

public enum ControlsStopControllingPacket implements CustomPacketPayload {
	INSTANCE;

	public static final StreamCodec<ByteBuf, ControlsStopControllingPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	public void handle(Player player) {
		ControlsHandler.stopControlling();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTROLS_ABORT.getType();
	}
}
