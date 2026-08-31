package com.simibubi.create.content.equipment.symmetryWand;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

import com.simibubi.create.AllPackets;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record SymmetryEffectPacket(BlockPos mirror, List<BlockPos> positions) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, SymmetryEffectPacket> STREAM_CODEC = StreamCodec.composite(
	        BlockPos.STREAM_CODEC, SymmetryEffectPacket::mirror,
			CatnipStreamCodecBuilders.list(BlockPos.STREAM_CODEC), SymmetryEffectPacket::positions,
	        SymmetryEffectPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SYMMETRY_EFFECT.getType();
	}

	public void handle(LocalPlayer player) {
		if (player.position().distanceTo(Vec3.atLowerCornerOf(mirror)) > 100)
			return;
		for (BlockPos to : positions)
			SymmetryHandler.drawEffect(mirror, to);
	}
}
