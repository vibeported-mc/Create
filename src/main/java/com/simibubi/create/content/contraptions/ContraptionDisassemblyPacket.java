package com.simibubi.create.content.contraptions;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ContraptionDisassemblyPacket(int entityId, StructureTransform transform) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ContraptionDisassemblyPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, ContraptionDisassemblyPacket::entityId,
			StructureTransform.STREAM_CODEC, ContraptionDisassemblyPacket::transform,
			ContraptionDisassemblyPacket::new
	);

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		AbstractContraptionEntity.handleDisassemblyPacket(this);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_DISASSEMBLE.getType();
	}
}
