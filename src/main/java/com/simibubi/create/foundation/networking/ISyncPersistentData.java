package com.simibubi.create.foundation.networking;

import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.HashSet;

import com.simibubi.create.AllPackets;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface ISyncPersistentData {

	void onPersistentDataUpdated();

	default void syncPersistentDataWithTracking(Entity self) {
		NetworkHelper.INSTANCE.sendToClientsTrackingEntity(self, new PersistentDataPacket(self));
	}

	record PersistentDataPacket(int entityId, CompoundTag readData) implements CustomPacketPayload {
		public static final StreamCodec<FriendlyByteBuf, PersistentDataPacket> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, PersistentDataPacket::entityId,
				ByteBufCodecs.COMPOUND_TAG, PersistentDataPacket::readData,
				PersistentDataPacket::new
		);

		public PersistentDataPacket(Entity entity) {
			this(entity.getId(), entity.getPersistentData());
		}

		@OnlyIn(Dist.CLIENT)
		public void handle(LocalPlayer player) {
			Entity entityByID = player.level().getEntity(entityId);
			CompoundTag data = entityByID.getPersistentData();
			new HashSet<>(data.getAllKeys()).forEach(data::remove);
			data.merge(readData);
			if (!(entityByID instanceof ISyncPersistentData))
				return;
			((ISyncPersistentData) entityByID).onPersistentDataUpdated();
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return AllPackets.PERSISTENT_DATA.getType();
		}
	}

}
