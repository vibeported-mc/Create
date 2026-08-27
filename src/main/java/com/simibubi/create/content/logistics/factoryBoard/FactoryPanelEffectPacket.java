package com.simibubi.create.content.logistics.factoryBoard;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllBlocks;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		ClientLevel level = Minecraft.getInstance().level;
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
