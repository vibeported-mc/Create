package com.simibubi.create.foundation.gui.menu;

import net.neoforged.neoforge.transfer.item.ItemResource;
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

	@Override
	public void handle(ServerPlayer player) {
		if (player.containerMenu instanceof GhostItemMenu<?> menu) {
			menu.ghostInventory.set(slot, ItemResource.of(item), item.getCount());
			menu.getSlot(36 + slot)
					.setChanged();
			}
			if (player.containerMenu instanceof StockKeeperCategoryMenu menu
				&& (item.isEmpty() || item.getItem() instanceof FilterItem)) {
				menu.proxyInventory.set(slot, ItemResource.of(item), item.getCount());
				menu.getSlot(36 + slot)
					.setChanged();
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SUBMIT_GHOST_ITEM.getType();
	}
}
