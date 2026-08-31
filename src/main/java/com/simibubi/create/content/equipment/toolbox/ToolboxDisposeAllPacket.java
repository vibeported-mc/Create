package com.simibubi.create.content.equipment.toolbox;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.AllPackets;

import net.createmod.catnip.api.nbt.NBTHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ToolboxDisposeAllPacket(BlockPos toolboxPos) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, ToolboxDisposeAllPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
			ToolboxDisposeAllPacket::new, ToolboxDisposeAllPacket::toolboxPos
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.TOOLBOX_DISPOSE_ALL.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		Level world = player.level();
		BlockEntity blockEntity = world.getBlockEntity(toolboxPos);

		double maxRange = ToolboxHandler.getMaxRange(player);
		if (player.distanceToSqr(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
				* maxRange)
			return;
		if (!(blockEntity instanceof ToolboxBlockEntity toolbox))
			return;

		CompoundTag compound = player.getPersistentData()
				.getCompoundOrEmpty("CreateToolboxData");
		MutableBoolean sendData = new MutableBoolean(false);

		toolbox.inventory.inLimitedMode(inventory -> {
			for (int i = 0; i < 36; i++) {
				String key = String.valueOf(i);
				if (compound.contains(key) && NBTHelper.readBlockPos(compound.getCompoundOrEmpty(key), "Pos")
					.equals(toolboxPos)) {
					ToolboxHandler.unequip(player, i, true);
					sendData.setTrue();
				}

				ItemStack itemStack = player.getInventory().getItem(i);
				ItemStack remainder;
				try (Transaction transaction = Transaction.openRoot()) {
					int transferred = itemStack.isEmpty() ? 0 : ResourceHandlerUtil.insertStacking(toolbox.inventory, ItemResource.of(itemStack), itemStack.getCount(), transaction);
					remainder = transferred == itemStack.getCount() ? ItemStack.EMPTY : itemStack.copyWithCount(itemStack.getCount() - transferred);
					transaction.commit();
				}
				if (remainder.getCount() != itemStack.getCount())
					player.getInventory().setItem(i, remainder);
			}
		});

		if (sendData.booleanValue())
			ToolboxHandler.syncData(player);
	}
}
