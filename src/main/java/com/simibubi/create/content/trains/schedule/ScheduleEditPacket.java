package com.simibubi.create.content.trains.schedule;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ScheduleEditPacket(Schedule schedule) implements SelfHandlingPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ScheduleEditPacket> STREAM_CODEC = Schedule.STREAM_CODEC.map(
			ScheduleEditPacket::new, ScheduleEditPacket::schedule
	);

	@Override
	public void handle(ServerPlayer sender) {
		ItemStack mainHandItem = sender.getMainHandItem();
		if (!AllItems.SCHEDULE.isIn(mainHandItem))
			return;

		if (schedule.entries.isEmpty()) {
			mainHandItem.remove(AllDataComponents.TRAIN_SCHEDULE);
		} else
			mainHandItem.set(AllDataComponents.TRAIN_SCHEDULE, schedule.write(sender.registryAccess()));

		sender.getCooldowns()
				.addCooldown(mainHandItem.getItem(), 5);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CONFIGURE_SCHEDULE.getType();
	}
}
