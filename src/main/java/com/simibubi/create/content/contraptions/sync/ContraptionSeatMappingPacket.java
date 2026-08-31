package com.simibubi.create.content.contraptions.sync;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ContraptionSeatMappingPacket(int entityId, Map<UUID, Integer> mapping, int dismountedId) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ContraptionSeatMappingPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, ContraptionSeatMappingPacket::entityId,
			ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.INT), ContraptionSeatMappingPacket::mapping,
			ByteBufCodecs.INT, ContraptionSeatMappingPacket::dismountedId,
	        ContraptionSeatMappingPacket::new
	);

	public ContraptionSeatMappingPacket {
		mapping = Map.copyOf(mapping);
	}

	public ContraptionSeatMappingPacket(int entityID, Map<UUID, Integer> mapping) {
		this(entityID, mapping, -1);
	}

	public void handle(LocalPlayer player) {
		Entity entityByID = player.level().getEntity(entityId);
		if (!(entityByID instanceof AbstractContraptionEntity contraptionEntity))
			return;

		if (dismountedId == player.getId()) {
			Vec3 transformedVector = contraptionEntity.getPassengerPosition(player, 1);
			if (transformedVector != null)
				player.getPersistentData()
						.put("ContraptionDismountLocation", VecHelper.writeNBT(transformedVector));
		}

		contraptionEntity.getContraption().setSeatMapping(mapping);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_SEAT_MAPPING.getType();
	}
}
