package com.simibubi.create.content.logistics.factoryBoard;

import net.minecraft.world.level.Level;

import com.simibubi.create.foundation.utility.ClientAccess;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllBlocks;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

public record FactoryPanelEffectPacket(FactoryPanelPosition fromPos, FactoryPanelPosition toPos, boolean success) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, FactoryPanelEffectPacket> STREAM_CODEC = StreamCodec.composite(
		FactoryPanelPosition.STREAM_CODEC, FactoryPanelEffectPacket::fromPos,
		FactoryPanelPosition.STREAM_CODEC, FactoryPanelEffectPacket::toPos,
		ByteBufCodecs.BOOL, FactoryPanelEffectPacket::success,
	    FactoryPanelEffectPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.FACTORY_PANEL_EFFECT.getType();
	}

	public void handle(Player player) {
		Level level = ClientAccess.level();
		BlockState blockState = level.getBlockState(fromPos.pos());
		if (!AllBlocks.FACTORY_GAUGE.has(blockState))
			return;
		FactoryPanelBehaviour panelBehaviour = FactoryPanelBehaviour.at(level, toPos);
		if (panelBehaviour != null) {
			panelBehaviour.bulb.setValue(1);
			FactoryPanelConnection connection = panelBehaviour.targetedBy.get(fromPos);
			if (connection != null)
				connection.success = success;
		}
	}
}
