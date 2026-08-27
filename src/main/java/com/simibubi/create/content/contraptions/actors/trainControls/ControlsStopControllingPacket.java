package com.simibubi.create.content.contraptions.actors.trainControls;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum ControlsStopControllingPacket implements CustomPacketPayload {
	INSTANCE;

	public static final StreamCodec<ByteBuf, ControlsStopControllingPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		ControlsHandler.stopControlling();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTROLS_ABORT.getType();
	}
}
