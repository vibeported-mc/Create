package com.simibubi.create.content.trains.station;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class GlobalStation extends SingleBlockEntityEdgePoint {

	public String name;
	public WeakReference<Train> nearestTrain;
	public boolean assembling;

	public Map<BlockPos, GlobalPackagePort> connectedPorts;

	public GlobalStation() {
		name = "Track Station";
		nearestTrain = new WeakReference<>(null);
		connectedPorts = new HashMap<>();
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		super.blockEntityAdded(blockEntity, front);
		BlockState state = blockEntity.getBlockState();
		assembling =
			state != null && state.hasProperty(StationBlock.ASSEMBLING) && state.getValue(StationBlock.ASSEMBLING);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, registries, migration, dimensions);
		name = nbt.getStringOr("Name", "");
		assembling = nbt.getBooleanOr("Assembling", false);
		nearestTrain = new WeakReference<>(null);

		connectedPorts.clear();
		ListTag portList = nbt.getListOrEmpty("Ports");
		NBTHelper.iterateCompoundList(portList, c -> {
			GlobalPackagePort port = new GlobalPackagePort();
			port.address = c.getStringOr("Address", "");
			NbtValueIO.deserialize(port.offlineBuffer, c.getCompoundOrEmpty("OfflineBuffer"), registries);
			port.primed = c.getBooleanOr("Primed", false);
			connectedPorts.put(NBTHelper.readBlockPos(c, "Pos"), port);
		});
	}

	@Override
	public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.read(buffer, dimensions);
		name = buffer.readUtf();
		assembling = buffer.readBoolean();
		if (buffer.readBoolean())
			blockEntityPos = buffer.readBlockPos();
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		super.write(nbt, registries, dimensions);
		nbt.putString("Name", name);
		nbt.putBoolean("Assembling", assembling);

		nbt.put("Ports", NBTHelper.writeCompoundList(connectedPorts.entrySet(), e -> {
			CompoundTag c = new CompoundTag();
			c.putString("Address", e.getValue().address);
			c.put("OfflineBuffer", NbtValueIO.serialize(e.getValue().offlineBuffer, registries));
			c.putBoolean("Primed", e.getValue().primed);
			c.store("Pos", BlockPos.CODEC, e.getKey());
			return c;
		}));
	}

	@Override
	public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.write(buffer, dimensions);
		buffer.writeUtf(name);
		buffer.writeBoolean(assembling);
		buffer.writeBoolean(blockEntityPos != null);
		if (blockEntityPos != null)
			buffer.writeBlockPos(blockEntityPos);
	}

	public boolean canApproachFrom(TrackNode side) {
		return isPrimary(side) && !assembling;
	}

	@Override
	public boolean canNavigateVia(TrackNode side) {
		return super.canNavigateVia(side) && !assembling;
	}

	public void reserveFor(Train train) {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null
			|| nearestTrain.navigation.distanceToDestination > train.navigation.distanceToDestination)
			this.nearestTrain = new WeakReference<>(train);
	}

	public void cancelReservation(Train train) {
		if (nearestTrain.get() == train)
			nearestTrain = new WeakReference<>(null);
	}

	public void trainDeparted(Train train) {
		cancelReservation(train);
	}

	@Nullable
	public Train getPresentTrain() {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null || nearestTrain.getCurrentStation() != this)
			return null;
		return nearestTrain;
	}

	@Nullable
	public Train getImminentTrain() {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null)
			return nearestTrain;
		if (nearestTrain.getCurrentStation() == this)
			return nearestTrain;
		if (!nearestTrain.navigation.isActive())
			return null;
		if (nearestTrain.navigation.distanceToDestination > 30)
			return null;
		return nearestTrain;
	}

	@Nullable
	public Train getNearestTrain() {
		return this.nearestTrain.get();
	}

	public void runMailTransfer() {
		Train train = getPresentTrain();
		if (train == null || connectedPorts.isEmpty())
			return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Level level = server.getLevel(getBlockEntityDimension());

		for (Carriage carriage : train.carriages) {
			ResourceHandler<ItemResource> carriageInventory = carriage.storage.getAllItems();
			if (carriageInventory == null)
				continue;

			// Import from station
			for (Entry<BlockPos, GlobalPackagePort> entry : connectedPorts.entrySet()) {
				GlobalPackagePort port = entry.getValue();
				BlockPos pos = entry.getKey();
				PostboxBlockEntity box = null;

				ResourceHandler<ItemResource> postboxInventory = port.offlineBuffer;
				if (level != null && level.isLoaded(pos)
					&& level.getBlockEntity(pos) instanceof PostboxBlockEntity ppbe) {
					postboxInventory = ppbe.inventory;
					box = ppbe;
				}

				for (int slot = 0; slot < postboxInventory.size(); slot++) {
					ItemStack stack = ItemUtil.getStack(postboxInventory, slot);
					if (!PackageItem.isPackage(stack))
						continue;
					if (PackageItem.matchAddress(stack, port.address))
						continue;

					boolean sent;

					// Taken out of the postbox and put into the carriage together, so a package that
					// only half fits stays where it was rather than going missing.
					try (Transaction transaction = Transaction.openRoot()) {
						ItemResource packaged = ItemResource.of(stack);
						sent = ResourceHandlerUtil.insertStacking(carriageInventory, packaged, stack.getCount(),
							transaction) == stack.getCount();

						if (sent) {
							if (!packaged.isEmpty())
								postboxInventory.extract(slot, packaged, stack.getCount(), transaction);
							transaction.commit();
						}
					}

					if (box != null)
						box.computerBehaviour.prepareComputerEvent(new PackageEvent(stack, "package_sent"));
					if (!sent)
						continue;

					if (box == null) {
						port.primed = true;
					} else {
						box.spawnParticles();
					}

					Create.RAILWAYS.markTracksDirty();
				}
			}

			// Export to station
			for (int slot = 0; slot < carriageInventory.size(); slot++) {
				ItemStack stack = ItemUtil.getStack(carriageInventory, slot);
				if (!PackageItem.isPackage(stack))
					continue;

				for (Entry<BlockPos, GlobalPackagePort> entry : connectedPorts.entrySet()) {
					GlobalPackagePort port = entry.getValue();
					BlockPos pos = entry.getKey();
					PostboxBlockEntity box = null;

					if (!PackageItem.matchAddress(stack, port.address))
						continue;

					ResourceHandler<ItemResource> postboxInventory = port.offlineBuffer;
					if (level != null && level.isLoaded(pos)
						&& level.getBlockEntity(pos) instanceof PostboxBlockEntity ppbe) {
						postboxInventory = ppbe.inventory;
						box = ppbe;
					}

					boolean received;

					// Likewise on the way back: out of the carriage and into the postbox at once.
					try (Transaction transaction = Transaction.openRoot()) {
						ItemResource packaged = ItemResource.of(stack);
						received = ResourceHandlerUtil.insertStacking(postboxInventory, packaged, stack.getCount(),
							transaction) == stack.getCount();

						if (received) {
							if (!packaged.isEmpty())
								carriageInventory.extract(slot, packaged, stack.getCount(), transaction);
							transaction.commit();
						}
					}

					if (box != null)
						box.computerBehaviour.prepareComputerEvent(new PackageEvent(stack, "package_received"));
					if (!received)
						continue;

					if (box == null) {
						port.primed = true;
					} else {
						box.spawnParticles();
					}

					Create.RAILWAYS.markTracksDirty();

					break;
				}
			}

		}
	}

}
