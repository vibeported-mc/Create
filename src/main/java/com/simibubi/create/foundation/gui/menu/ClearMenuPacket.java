package com.simibubi.create.foundation.gui.menu;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public enum ClearMenuPacket implements SelfHandlingPayload {
	INSTANCE;

	public static final StreamCodec<ByteBuf, ClearMenuPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CLEAR_CONTAINER.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		if (player == null)
			return;
		if (!(player.containerMenu instanceof IClearableMenu))
			return;
		((IClearableMenu) player.containerMenu).clearContents();
	}
}
