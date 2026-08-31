package com.simibubi.create.content.equipment.clipboard;

import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.minecraft.core.UUIDUtil;
import java.util.List;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.AddressEditBoxHelper;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ClipboardBlockEntity extends SmartBlockEntity {
	private UUID lastEdit;

	public ClipboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void initialize() {
		super.initialize();
		updateWrittenState();
	}

	public void onEditedBy(Player player) {
		lastEdit = player.getUUID();
		notifyUpdate();
		updateWrittenState();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide())
			PlatformHelper.INSTANCE.executeOnClientOnly(() -> this::advertiseToAddressHelper);
	}

	public void updateWrittenState() {
		BlockState blockState = getBlockState();
		if (!AllBlocks.CLIPBOARD.has(blockState))
			return;
		if (level.isClientSide())
			return;
		boolean isWritten = blockState.getValue(ClipboardBlock.WRITTEN);
		boolean shouldBeWritten = components().has(AllDataComponents.CLIPBOARD_CONTENT);
		if (isWritten == shouldBeWritten)
			return;
		level.setBlockAndUpdate(worldPosition, blockState.setValue(ClipboardBlock.WRITTEN, shouldBeWritten));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);

		if (clientPacket) {
			DataComponentMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), components())
				.result()
				.ifPresent(encoded -> tag.put("components", encoded));

			if (lastEdit != null)
				tag.store("LastEdit", UUIDUtil.CODEC, lastEdit);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);

		if (clientPacket) {
			if (tag.contains("components"))
				DataComponentMap.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompoundOrEmpty("components"))
					.result()
					.map(Pair::getFirst)
					.ifPresent(this::setComponents);

			PlatformHelper.INSTANCE.executeOnClientOnly(() -> () -> readClientSide(tag));
		}
	}

	private void readClientSide(CompoundTag tag) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.gui.screen() instanceof ClipboardScreen cs))
			return;
		if (tag.contains("LastEdit") && tag.read("LastEdit", UUIDUtil.CODEC).orElse(null)
			.equals(mc.player.getUUID()))
			return;
		if (!worldPosition.equals(cs.targetedBlock))
			return;
		cs.reopenWith(components().getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY));
	}

	private void advertiseToAddressHelper() {
		AddressEditBoxHelper.advertiseClipboard(this);
	}

	@Override
	public void setComponents(DataComponentMap components) {
		super.setComponents(components);
	}
}
