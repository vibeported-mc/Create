package com.simibubi.create.foundation.blockEntity;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import com.simibubi.create.foundation.utility.NbtValueIO;
import org.jspecify.annotations.NullMarked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@NullMarked
public abstract class SyncedBlockEntity extends BlockEntity {
	public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Create's block entities are written against {@link CompoundTag} throughout, and share that
	 * shape with packets and contraption storage. Minecraft 26.2 saves through {@link ValueOutput}
	 * and {@link ValueInput} instead, so the whole tag is moved across the boundary here and every
	 * subclass keeps the hooks it already has.
	 */
	@Override
	protected final void saveAdditional(ValueOutput output) {
		CompoundTag tag = new CompoundTag();
		HolderLookup.Provider registries = level != null ? level.registryAccess() : RegistryAccess.EMPTY;
		saveAdditional(tag, registries);
		NbtValueIO.store(output, tag);
	}

	@Override
	protected final void loadAdditional(ValueInput input) {
		loadAdditional(NbtValueIO.read(input), input.lookup());
	}

	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	}

	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return writeClient(new CompoundTag(), registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		readClient(NbtValueIO.read(input), input.lookup());
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		readClient(NbtValueIO.read(input), input.lookup());
	}

	// Special handling for client update packets
	public void readClient(CompoundTag tag, HolderLookup.Provider registries) {
		loadAdditional(tag, registries);
	}

	// Special handling for client update packets
	public CompoundTag writeClient(CompoundTag tag, HolderLookup.Provider registries) {
		saveAdditional(tag, registries);
		return tag;
	}

	public void sendData() {
		if (level instanceof ServerLevel serverLevel)
			serverLevel.getChunkSource().blockChanged(getBlockPos());
	}

	public void notifyUpdate() {
		setChanged();
		sendData();
	}

	public HolderGetter<Block> blockHolderGetter() {
		return level != null ? level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK;
	}
}
