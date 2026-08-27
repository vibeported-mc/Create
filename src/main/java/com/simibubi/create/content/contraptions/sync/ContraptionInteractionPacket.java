package com.simibubi.create.content.contraptions.sync;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

public record ContraptionInteractionPacket(InteractionHand hand, int target, BlockPos localPos, Direction face) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, ContraptionInteractionPacket> STREAM_CODEC = StreamCodec.composite(
			CatnipStreamCodecBuilders.nullable(CatnipStreamCodecs.HAND), ContraptionInteractionPacket::hand,
			ByteBufCodecs.INT, ContraptionInteractionPacket::target,
			BlockPos.STREAM_CODEC, ContraptionInteractionPacket::localPos,
			Direction.STREAM_CODEC, ContraptionInteractionPacket::face,
			ContraptionInteractionPacket::new
	);

	public ContraptionInteractionPacket(AbstractContraptionEntity target, InteractionHand hand, BlockPos localPos, Direction side) {
		this(hand, target.getId(), localPos, side);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONTRAPTION_INTERACT.getType();
	}

	@Override
	public void handle(ServerPlayer sender) {
		if (sender == null)
			return;
		Entity entityByID = sender.level().getEntity(target);
		if (!(entityByID instanceof AbstractContraptionEntity contraptionEntity))
			return;
		AABB bb = contraptionEntity.getBoundingBox();
		double boundsExtra = Math.max(bb.getXsize(), bb.getYsize());
		double d = sender.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 10 + boundsExtra;
		if (!sender.hasLineOfSight(entityByID))
			d -= 3;
		d *= d;
		if (sender.distanceToSqr(entityByID) > d)
			return;
		if (contraptionEntity.handlePlayerInteraction(sender, localPos, face, hand))
			sender.swing(hand, true);
	}
}
