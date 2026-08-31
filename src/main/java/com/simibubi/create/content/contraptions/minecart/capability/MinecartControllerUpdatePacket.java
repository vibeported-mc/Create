package com.simibubi.create.content.contraptions.minecart.capability;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

public record MinecartControllerUpdatePacket(int entityId, @Nullable CompoundTag nbt) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, MinecartControllerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, MinecartControllerUpdatePacket::entityId,
			CatnipStreamCodecBuilders.nullable(ByteBufCodecs.COMPOUND_TAG), MinecartControllerUpdatePacket::nbt,
			MinecartControllerUpdatePacket::new
	);

	public MinecartControllerUpdatePacket(MinecartController controller, @NotNull HolderLookup.Provider registries) {
		this(controller.cart().getId(), controller.isEmpty() ? null : controller.serializeNBT(registries));
	}

	public void handle(LocalPlayer player) {
		Entity entityByID = player.level().getEntity(entityId);
		if (entityByID == null)
			return;
		if (entityByID.hasData(AllAttachmentTypes.MINECART_CONTROLLER)) {
			if (nbt == null) {
				entityByID.removeData(AllAttachmentTypes.MINECART_CONTROLLER);
			} else {
				MinecartController controller = entityByID.getData(AllAttachmentTypes.MINECART_CONTROLLER);
				controller.deserializeNBT(player.registryAccess(), nbt);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.MINECART_CONTROLLER.getType();
	}
}
