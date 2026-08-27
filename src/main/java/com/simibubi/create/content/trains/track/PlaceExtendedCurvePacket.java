package com.simibubi.create.content.trains.track;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllTags;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record PlaceExtendedCurvePacket(boolean mainHand, boolean ctrlDown) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, PlaceExtendedCurvePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PlaceExtendedCurvePacket::mainHand,
			ByteBufCodecs.BOOL, PlaceExtendedCurvePacket::ctrlDown,
	        PlaceExtendedCurvePacket::new
	);

	@Override
	public void handle(ServerPlayer sender) {
		ItemStack stack = sender.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
		if (!AllTags.AllBlockTags.TRACKS.matches(stack))
			return;
		stack.set(AllDataComponents.TRACK_EXTENDED_CURVE, true);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PLACE_CURVED_TRACK.getType();
	}
}
