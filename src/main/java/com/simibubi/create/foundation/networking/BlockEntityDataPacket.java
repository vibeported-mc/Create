package com.simibubi.create.foundation.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A server to client version of {@link BlockEntityConfigurationPacket}
 */
public abstract class BlockEntityDataPacket<BE extends SyncedBlockEntity> implements CustomPacketPayload {
	protected final BlockPos pos;

	public BlockEntityDataPacket(BlockPos pos) {
		this.pos = pos;
	}

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		BlockEntity blockEntity = player.clientLevel.getBlockEntity(pos);

		if (blockEntity instanceof SyncedBlockEntity) {
			handlePacket((BE) blockEntity);
		}
	}

	protected abstract void handlePacket(BE blockEntity);
}
