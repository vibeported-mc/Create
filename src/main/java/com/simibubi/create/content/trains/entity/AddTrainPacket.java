package com.simibubi.create.content.trains.entity;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.CreateClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record AddTrainPacket(Train train) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, AddTrainPacket> STREAM_CODEC = Train.STREAM_CODEC.map(AddTrainPacket::new, AddTrainPacket::train);

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		CreateClient.RAILWAYS.trains.put(train.id, train);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.ADD_TRAIN.getType();
	}
}
