package com.simibubi.create.foundation.gui.menu;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record GhostItemSubmitPacket(ItemStack item, int slot) implements SelfHandlingPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, GhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(
	        ItemStack.OPTIONAL_STREAM_CODEC, GhostItemSubmitPacket::item,
			ByteBufCodecs.INT, GhostItemSubmitPacket::slot,
	        GhostItemSubmitPacket::new
	);

	/**
	 * Through the slot, not into the handler behind it: a slot over a resource handler writes the
	 * stack it last handed out back over the handler when told something changed, so setting the
	 * handler and then calling {@code setChanged} undid the submission straight away - an item
	 * dragged in from JEI or EMI showed on the client and was never kept.
	 */
	@Override
	public void handle(ServerPlayer player) {
		if (player.containerMenu instanceof GhostItemMenu<?> menu) {
			menu.getSlot(36 + slot)
				.set(item);
		}
		if (player.containerMenu instanceof StockKeeperCategoryMenu menu
			&& (item.isEmpty() || item.getItem() instanceof FilterItem)) {
			menu.getSlot(36 + slot)
				.set(item);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SUBMIT_GHOST_ITEM.getType();
	}
}
