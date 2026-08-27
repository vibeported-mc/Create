package com.simibubi.create.content.contraptions.actors.contraptionControls;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ContraptionDisableActorPacket(int entityId, ItemStack filter, boolean enable) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ContraptionDisableActorPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, ContraptionDisableActorPacket::entityId,
			ItemStack.OPTIONAL_STREAM_CODEC, ContraptionDisableActorPacket::filter,
			ByteBufCodecs.BOOL, ContraptionDisableActorPacket::enable,
	        ContraptionDisableActorPacket::new
	);

	@OnlyIn(Dist.CLIENT)
	public void handle(LocalPlayer player) {
		Entity entityByID = player.level().getEntity(entityId);
		if (!(entityByID instanceof AbstractContraptionEntity ace))
			return;

		Contraption contraption = ace.getContraption();
		List<ItemStack> disabledActors = contraption.getDisabledActors();
		if (filter.isEmpty())
			disabledActors.clear();

		if (!enable) {
			disabledActors.add(filter);
			contraption.setActorsActive(filter, false);
			return;
		}

		disabledActors.removeIf(next -> ContraptionControlsMovement.isSameFilter(next, filter) || next.isEmpty());

		contraption.setActorsActive(filter, true);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_ACTOR_TOGGLE.getType();
	}
}
