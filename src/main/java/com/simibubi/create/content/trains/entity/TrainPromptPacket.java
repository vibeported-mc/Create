package com.simibubi.create.content.trains.entity;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.trains.TrainHUD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TrainPromptPacket(Component text, boolean shadow) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, TrainPromptPacket> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, TrainPromptPacket::text,
			ByteBufCodecs.BOOL, TrainPromptPacket::shadow,
	        TrainPromptPacket::new
	);

	public void handle(Player player) {
		TrainHUD.currentPrompt = text;
		TrainHUD.currentPromptShadow = shadow;
		TrainHUD.promptKeepAlive = 30;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.S_TRAIN_PROMPT.getType();
	}
}
