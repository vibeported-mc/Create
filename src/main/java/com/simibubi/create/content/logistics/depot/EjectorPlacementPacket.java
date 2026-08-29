package com.simibubi.create.content.logistics.depot;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record EjectorPlacementPacket(int h, int v, BlockPos pos, Direction facing) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, EjectorPlacementPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, EjectorPlacementPacket::h,
			ByteBufCodecs.INT, EjectorPlacementPacket::v,
			BlockPos.STREAM_CODEC, EjectorPlacementPacket::pos,
			Direction.STREAM_CODEC, EjectorPlacementPacket::facing,
	        EjectorPlacementPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PLACE_EJECTOR.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();
		if (!world.isLoaded(pos))
			return;
		BlockEntity blockEntity = world.getBlockEntity(pos);
		BlockState state = world.getBlockState(pos);
		if (blockEntity instanceof EjectorBlockEntity)
			((EjectorBlockEntity) blockEntity).setTarget(h, v);
		if (AllBlocks.WEIGHTED_EJECTOR.has(state))
			world.setBlockAndUpdate(pos, state.setValue(EjectorBlock.HORIZONTAL_FACING, facing));
	}

	public record ClientBoundRequest(BlockPos pos) implements CustomPacketPayload {
		public static final StreamCodec<ByteBuf, ClientBoundRequest> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
				ClientBoundRequest::new, ClientBoundRequest::pos
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return AllPackets.S_PLACE_EJECTOR.getType();
		}

		@OnlyIn(Dist.CLIENT)
		public void handle(LocalPlayer player) {
			EjectorTargetHandler.flushSettings(pos);
		}
	}

}
