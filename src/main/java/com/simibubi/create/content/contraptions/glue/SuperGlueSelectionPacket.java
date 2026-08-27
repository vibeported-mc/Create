package com.simibubi.create.content.contraptions.glue;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public record SuperGlueSelectionPacket(BlockPos from, BlockPos to) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, SuperGlueSelectionPacket> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SuperGlueSelectionPacket::from,
			BlockPos.STREAM_CODEC, SuperGlueSelectionPacket::to,
			SuperGlueSelectionPacket::new
	);

	@Override
	public void handle(ServerPlayer player) {
		if (!player.isWithinBlockInteractionRange(to, 2))
			return;
		if (!to.closerThan(from, 25))
			return;

		Set<BlockPos> group = SuperGlueSelectionHelper.searchGlueGroup(player.level(), from, to, false);
		if (group == null)
			return;
		if (!group.contains(to))
			return;
		if (!SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, true))
			return;

		AABB bb = SuperGlueEntity.span(from, to);
		SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, false);
		SuperGlueEntity entity = new SuperGlueEntity(player.level(), bb);
		player.level().addFreshEntity(entity);
		entity.spawnParticles();

		AllAdvancements.SUPER_GLUE.awardTo(player);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.GLUE_IN_AREA.getType();
	}
}
