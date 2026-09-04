package com.simibubi.create.foundation.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A server to client version of {@link BlockEntityConfigurationPacket}
 */
public abstract class BlockEntityDataPacket<BE extends SyncedBlockEntity> implements CustomPacketPayload {
	protected final BlockPos pos;

	public BlockEntityDataPacket(BlockPos pos) {
		this.pos = pos;
	}

	public void handle(Player player) {
		BlockEntity blockEntity = player.level().getBlockEntity(pos);

		if (blockEntity instanceof SyncedBlockEntity) {
			handlePacket((BE) blockEntity);
		}
	}

	protected abstract void handlePacket(BE blockEntity);
}
