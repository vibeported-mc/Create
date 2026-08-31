package com.simibubi.create.content.equipment.toolbox;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ToolboxEquipPacket(BlockPos toolboxPos, int slot, int hotbarSlot) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, ToolboxEquipPacket> STREAM_CODEC = StreamCodec.composite(
			CatnipStreamCodecBuilders.nullable(BlockPos.STREAM_CODEC), ToolboxEquipPacket::toolboxPos,
			ByteBufCodecs.VAR_INT, ToolboxEquipPacket::slot,
			ByteBufCodecs.VAR_INT, ToolboxEquipPacket::hotbarSlot,
	        ToolboxEquipPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.TOOLBOX_EQUIP.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();

		if (toolboxPos == null) {
			ToolboxHandler.unequip(player, hotbarSlot, false);
			ToolboxHandler.syncData(player);
			return;
		}

		BlockEntity blockEntity = world.getBlockEntity(toolboxPos);

		double maxRange = ToolboxHandler.getMaxRange(player);
		if (player.distanceToSqr(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
				* maxRange)
			return;
		if (!(blockEntity instanceof ToolboxBlockEntity toolboxBlockEntity))
			return;

		ToolboxHandler.unequip(player, hotbarSlot, false);

		if (slot < 0 || slot >= 8) {
			ToolboxHandler.syncData(player);
			return;
		}

		ItemStack playerStack = player.getInventory().getItem(hotbarSlot);
		if (!playerStack.isEmpty() && !ToolboxInventory.canItemsShareCompartment(playerStack,
				toolboxBlockEntity.inventory.filters.get(slot))) {
			toolboxBlockEntity.inventory.inLimitedMode(inventory -> {
				ItemStack remainder;
				try (Transaction transaction = Transaction.openRoot()) {
					int transferred = playerStack.isEmpty() ? 0 : ResourceHandlerUtil.insertStacking(inventory, ItemResource.of(playerStack), playerStack.getCount(), transaction);
					remainder = transferred == playerStack.getCount() ? ItemStack.EMPTY : playerStack.copyWithCount(playerStack.getCount() - transferred);
					transaction.commit();
				}
				if (!remainder.isEmpty())
					try (Transaction transaction = Transaction.openRoot()) {
						int transferred2 = remainder.isEmpty() ? 0 : ResourceHandlerUtil.insertStacking(new ItemReturnInvWrapper(player.getInventory()), ItemResource.of(remainder), remainder.getCount(), transaction);
						remainder = transferred2 == remainder.getCount() ? ItemStack.EMPTY : remainder.copyWithCount(remainder.getCount() - transferred2);
						transaction.commit();
					}
				if (remainder.getCount() != playerStack.getCount())
					player.getInventory().setItem(hotbarSlot, remainder);
			});
		}

		CompoundTag compound = player.getPersistentData()
				.getCompoundOrEmpty("CreateToolboxData");
		String key = String.valueOf(hotbarSlot);

		CompoundTag data = new CompoundTag();
		data.putInt("Slot", slot);
		data.store("Pos", BlockPos.CODEC, toolboxPos);
		compound.put(key, data);

		player.getPersistentData()
				.put("CreateToolboxData", compound);

		toolboxBlockEntity.connectPlayer(slot, player, hotbarSlot);
		ToolboxHandler.syncData(player);
	}
}
