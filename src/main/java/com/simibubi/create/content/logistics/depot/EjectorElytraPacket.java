package com.simibubi.create.content.logistics.depot;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record EjectorElytraPacket(BlockPos pos) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, EjectorElytraPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
			EjectorElytraPacket::new, EjectorElytraPacket::pos
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.EJECTOR_ELYTRA.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();
		if (!world.isLoaded(pos))
			return;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof EjectorBlockEntity)
			((EjectorBlockEntity) blockEntity).deployElytra(player);
	}
}
