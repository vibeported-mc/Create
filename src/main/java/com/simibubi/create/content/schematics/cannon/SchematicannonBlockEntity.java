package com.simibubi.create.content.schematics.cannon;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import com.simibubi.create.foundation.utility.RegistryNbt;
import net.minecraft.core.component.DataComponentGetter;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CSchematics;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Clearable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
public class SchematicannonBlockEntity extends SmartBlockEntity implements MenuProvider, Clearable {
	public static final int NEIGHBOUR_CHECKING = 100;
	public static final int MAX_ANCHOR_DISTANCE = 256;

	// Inventory
	public SchematicannonInventory inventory;

	public boolean sendUpdate;
	// Sync
	public boolean dontUpdateChecklist;
	public int neighbourCheckCooldown;

	// Printer
	public SchematicPrinter printer;
	public ItemStack missingItem;
	public boolean positionNotLoaded;
	public boolean hasCreativeCrate;
	private int printerCooldown;
	private int skipsLeft;
	private boolean blockSkipped;

	public BlockPos previousTarget;
	public LinkedHashSet<ResourceHandler<ItemResource>> attachedInventories;
	public List<LaunchedItem> flyingBlocks;
	public MaterialChecklist checklist;

	// Gui information
	public int remainingFuel;
	public float bookPrintingProgress;
	public float schematicProgress;
	public String statusMsg;
	public State state;
	public int blocksPlaced;
	public int blocksToPlace;

	// Settings
	public int replaceMode;
	public boolean skipMissing;
	public boolean replaceBlockEntities;

	// Render
	public boolean firstRenderTick;
	public float defaultYaw;

	public SchematicannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(30);
		attachedInventories = new LinkedHashSet<>();
		flyingBlocks = new LinkedList<>();
		inventory = new SchematicannonInventory(this);
		statusMsg = "idle";
		this.state = State.STOPPED;
		replaceMode = 2;
		checklist = new MaterialChecklist();
		printer = new SchematicPrinter();
	}

	public void findInventories() {
		hasCreativeCrate = false;
		attachedInventories.clear();
		for (Direction facing : Iterate.directions) {

			if (!level.isLoaded(worldPosition.relative(facing)))
				continue;

			if (AllBlocks.CREATIVE_CRATE.has(level.getBlockState(worldPosition.relative(facing))))
				hasCreativeCrate = true;

			BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(facing));
			if (blockEntity != null) {
				ResourceHandler<ItemResource> capability =
					level.getCapability(Capabilities.Item.BLOCK, blockEntity.getBlockPos(), facing.getOpposite());
				if (capability != null) {
					attachedInventories.add(capability);
				}
			}
		}
	}

	@Override
	public void clearContent() {
		for (int slot = 0; slot < inventory.size(); slot++)
			inventory.set(slot, ItemResource.EMPTY, 0);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (!clientPacket) {
			NbtValueIO.deserialize(inventory, compound.getCompoundOrEmpty("Inventory"), registries);
		}

		// Gui information
		statusMsg = compound.getStringOr("Status", "");
		schematicProgress = compound.getFloatOr("Progress", 0);
		bookPrintingProgress = compound.getFloatOr("PaperProgress", 0);
		remainingFuel = compound.getIntOr("RemainingFuel", 0);
		String stateString = compound.getStringOr("State", "");
		state = stateString.isEmpty() ? State.STOPPED : State.valueOf(compound.getStringOr("State", ""));
		blocksPlaced = compound.getIntOr("AmountPlaced", 0);
		blocksToPlace = compound.getIntOr("AmountToPlace", 0);

		missingItem = null;
		if (compound.contains("MissingItem")) {
			compound.read("MissingItem", ItemStack.CODEC, RegistryNbt.ops(registries))
				.ifPresent(i -> missingItem = i);
		}

		// Settings
		SchematicannonOptions options = CatnipCodecUtils.decode(SchematicannonOptions.CODEC, registries, compound.getCompoundOrEmpty("Options"))
			.orElse(new SchematicannonOptions(2, false, false));
		replaceMode = options.replaceMode;
		skipMissing = options.skipMissing;
		replaceBlockEntities = options.replaceBlockEntities;

		// Printer & Flying Blocks
		if (compound.contains("Printer"))
			printer.fromTag(compound.getCompoundOrEmpty("Printer"), clientPacket);
		if (compound.contains("FlyingBlocks"))
			readFlyingBlocks(compound, registries);

		defaultYaw = compound.getFloatOr("DefaultYaw", 0);

		super.read(compound, registries, clientPacket);
	}

	protected void readFlyingBlocks(CompoundTag compound, HolderLookup.Provider registries) {
		ListTag tagBlocks = compound.getListOrEmpty("FlyingBlocks");
		if (tagBlocks.isEmpty())
			flyingBlocks.clear();

		boolean pastDead = false;

		for (int i = 0; i < tagBlocks.size(); i++) {
			CompoundTag c = tagBlocks.getCompoundOrEmpty(i);
			LaunchedItem launched = LaunchedItem.fromNBT(c, registries, blockHolderGetter());
			BlockPos readBlockPos = launched.target;

			// Always write to Server block entity
			if (level == null || !level.isClientSide()) {
				flyingBlocks.add(launched);
				continue;
			}

			// Delete all Client side blocks that are now missing on the server
			while (!pastDead && !flyingBlocks.isEmpty() && !flyingBlocks.get(0).target.equals(readBlockPos)) {
				flyingBlocks.remove(0);
			}

			pastDead = true;

			// Add new server side blocks
			if (i >= flyingBlocks.size()) {
				flyingBlocks.add(launched);
				continue;
			}

			// Don't do anything with existing
		}
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (!clientPacket) {
			compound.put("Inventory", NbtValueIO.serialize(inventory, registries));
			if (state == State.RUNNING) {
				compound.putBoolean("Running", true);
			}
		}

		// Gui information
		compound.putFloat("Progress", schematicProgress);
		compound.putFloat("PaperProgress", bookPrintingProgress);
		compound.putInt("RemainingFuel", remainingFuel);
		compound.putString("Status", statusMsg);
		compound.putString("State", state.name());
		compound.putInt("AmountPlaced", blocksPlaced);
		compound.putInt("AmountToPlace", blocksToPlace);

		if (missingItem != null)
			compound.store("MissingItem", ItemStack.OPTIONAL_CODEC, RegistryNbt.ops(registries), missingItem);

		// Settings
		Tag options = CatnipCodecUtils.encode(SchematicannonOptions.CODEC, registries, new SchematicannonOptions(replaceMode, skipMissing, replaceBlockEntities)).orElseThrow();
		compound.put("Options", options);

		// Printer & Flying Blocks
		CompoundTag printerData = new CompoundTag();
		printer.write(printerData);
		compound.put("Printer", printerData);

		ListTag tagFlyingBlocks = new ListTag();
		for (LaunchedItem b : flyingBlocks)
			tagFlyingBlocks.add(b.serializeNBT(registries));
		compound.put("FlyingBlocks", tagFlyingBlocks);

		compound.putFloat("DefaultYaw", defaultYaw);

		super.write(compound, registries, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (state != State.STOPPED && neighbourCheckCooldown-- <= 0) {
			neighbourCheckCooldown = NEIGHBOUR_CHECKING;
			findInventories();
		}

		firstRenderTick = true;
		previousTarget = printer.getCurrentTarget();
		tickFlyingBlocks();

		if (level.isClientSide())
			return;

		// Update Fuel and Paper
		tickPaperPrinter();
		refillFuelIfPossible();

		// Update Printer
		skipsLeft = 1000;
		blockSkipped = true;

		while (blockSkipped && skipsLeft-- > 0)
			tickPrinter();

		schematicProgress = 0;
		if (blocksToPlace > 0)
			schematicProgress = (float) blocksPlaced / blocksToPlace;

		// Update Client block entity
		if (sendUpdate) {
			sendUpdate = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 6);
		}
	}

	public CSchematics config() {
		return AllConfigs.server().schematics;
	}

	protected void tickPrinter() {
		ItemStack blueprint = ItemUtil.getStack(inventory, 0);
		blockSkipped = false;

		if (blueprint.isEmpty() && !statusMsg.equals("idle") && ItemUtil.getStack(inventory, 1)
			.isEmpty()) {
			state = State.STOPPED;
			statusMsg = "idle";
			sendUpdate = true;
			return;
		}

		// Skip if not Active
		if (state == State.STOPPED) {
			if (printer.isLoaded())
				resetPrinter();
			return;
		}

		if (state == State.PAUSED && !positionNotLoaded && missingItem == null && remainingFuel > 0)
			return;

		// Initialize Printer
		if (!printer.isLoaded()) {
			initializePrinter(blueprint);
			return;
		}

		// Cooldown from last shot
		if (printerCooldown > 0) {
			printerCooldown--;
			return;
		}

		// Check Fuel
		if (remainingFuel <= 0 && !hasCreativeCrate) {
			refillFuelIfPossible();
			if (remainingFuel <= 0) {
				state = State.PAUSED;
				statusMsg = "noGunpowder";
				sendUpdate = true;
				return;
			}
		}

		if (hasCreativeCrate) {
			remainingFuel = 0;
			if (missingItem != null) {
				missingItem = null;
				state = State.RUNNING;
			}
		}

		// Update Target
		if (missingItem == null && !positionNotLoaded) {
			if (!printer.advanceCurrentPos()) {
				finishedPrinting();
				return;
			}
			sendUpdate = true;
		}

		// Check block
		if (!getLevel().isLoaded(printer.getCurrentTarget())) {
			positionNotLoaded = true;
			statusMsg = "targetNotLoaded";
			state = State.PAUSED;
			return;
		} else {
			if (positionNotLoaded) {
				positionNotLoaded = false;
				state = State.RUNNING;
			}
		}

		// Get item requirement
		ItemRequirement requirement = printer.getCurrentRequirement();
		if (requirement.isInvalid() || !printer.shouldPlaceCurrent(level, this::shouldPlace)) {
			sendUpdate = !statusMsg.equals("searching");
			statusMsg = "searching";
			blockSkipped = true;
			return;
		}

		// Find item
		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		if (!requirement.isEmpty()) {
			for (ItemRequirement.StackRequirement required : requiredItems) {
				if (!grabItemsFromAttachedInventories(required, true)) {
					if (skipMissing) {
						statusMsg = "skipping";
						blockSkipped = true;
						if (missingItem != null) {
							missingItem = null;
							state = State.RUNNING;
						}
						return;
					}

					missingItem = required.stack;
					state = State.PAUSED;
					statusMsg = "missingBlock";
					return;
				}
			}

			for (ItemRequirement.StackRequirement required : requiredItems)
				grabItemsFromAttachedInventories(required, false);
		}

		// Success
		state = State.RUNNING;
		ItemStack icon = requirement.isEmpty() || requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).stack;
		printer.handleCurrentTarget((target, blockState, blockEntity) -> {
			// Launch block
			statusMsg = blockState.getBlock() != Blocks.AIR ? "placing" : "clearing";
			launchBlockOrBelt(target, icon, blockState, blockEntity);
		}, (target, entity) -> {
			// Launch entity
			statusMsg = "placing";
			launchEntity(target, icon, entity);
		});

		printerCooldown = config().schematicannonDelay.get();
		remainingFuel -= 1;
		sendUpdate = true;
		missingItem = null;
	}

	public int getShotsPerGunpowder() {
		return hasCreativeCrate ? 0 : config().schematicannonShotsPerGunpowder.get();
	}

	protected void initializePrinter(ItemStack blueprint) {
		if (!blueprint.has(AllDataComponents.SCHEMATIC_ANCHOR)) {
			state = State.STOPPED;
			statusMsg = "schematicInvalid";
			sendUpdate = true;
			return;
		}

		if (!blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)) {
			state = State.STOPPED;
			statusMsg = "schematicNotPlaced";
			sendUpdate = true;
			return;
		}

		// Load blocks into reader
		printer.loadSchematic(blueprint, level, true);

		if (printer.isErrored()) {
			state = State.STOPPED;
			statusMsg = "schematicErrored";
			inventory.set(0, ItemResource.EMPTY, 0);
			ItemStack stack = new ItemStack(AllItems.EMPTY_SCHEMATIC.get());
			inventory.set(1, ItemResource.of(stack), stack.getCount());
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		if (printer.isWorldEmpty()) {
			state = State.STOPPED;
			statusMsg = "schematicExpired";
			inventory.set(0, ItemResource.EMPTY, 0);
			ItemStack stack = new ItemStack(AllItems.EMPTY_SCHEMATIC.get());
			inventory.set(1, ItemResource.of(stack), stack.getCount());
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		if (!printer.getAnchor()
			.closerThan(getBlockPos(), MAX_ANCHOR_DISTANCE)) {
			state = State.STOPPED;
			statusMsg = "targetOutsideRange";
			printer.resetSchematic();
			sendUpdate = true;
			return;
		}

		state = State.PAUSED;
		statusMsg = "ready";
		updateChecklist();
		sendUpdate = true;
		blocksToPlace += blocksPlaced;
	}

	protected ItemStack getItemForBlock(BlockState blockState) {
		Item item = BlockItem.BY_BLOCK.getOrDefault(blockState.getBlock(), Items.AIR);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	protected boolean grabItemsFromAttachedInventories(ItemRequirement.StackRequirement required, boolean simulate) {
		if (hasCreativeCrate)
			return true;

		attachedInventories.removeIf(Objects::isNull);

		ItemUseType usage = required.usage;

		// Find and apply damage
		if (usage == ItemUseType.DAMAGE) {
			for (ResourceHandler<ItemResource> cap : attachedInventories) {
				if (cap == null)
					cap = EmptyResourceHandler.instance();
				for (int slot = 0; slot < cap.size(); slot++) {
					ItemStack extractItem;
					try (Transaction transaction = Transaction.openRoot()) {
						ItemResource transferredResource = cap.getResource(slot);
						int transferred = transferredResource.isEmpty() ? 0 : cap.extract(slot, transferredResource, 1, transaction);
						extractItem = transferred <= 0 ? ItemStack.EMPTY : transferredResource.toStack(transferred);
					}
					if (!required.matches(extractItem))
						continue;
					if (!extractItem.isDamageableItem())
						continue;

					if (!simulate) {
						ItemStack stack;
						try (Transaction transaction = Transaction.openRoot()) {
							ItemResource transferred2Resource = cap.getResource(slot);
							int transferred2 = transferred2Resource.isEmpty() ? 0 : cap.extract(slot, transferred2Resource, 1, transaction);
							stack = transferred2 <= 0 ? ItemStack.EMPTY : transferred2Resource.toStack(transferred2);
							transaction.commit();
						}
						stack.setDamageValue(stack.getDamageValue() + 1);
						if (stack.getDamageValue() <= stack.getMaxDamage()) {
							if (ItemUtil.getStack(cap, slot)
								.isEmpty())
								try (Transaction transaction = Transaction.openRoot()) {
									int transferred = stack.isEmpty() ? 0 : cap.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
									transaction.commit();
								}
							else
								try (Transaction transaction = Transaction.openRoot()) {
									int transferred2 = stack.isEmpty() ? 0 : cap.insert(ItemResource.of(stack), stack.getCount(), transaction);
									transaction.commit();
								}
						}
					}

					return true;
				}
			}

			return false;
		}

		// Find and remove
		boolean success = false;
		int amountFound = 0;
		for (ResourceHandler<ItemResource> cap : attachedInventories) {
			if (cap == null)
				cap = EmptyResourceHandler.instance();
			amountFound += ItemHelper
				.extract(cap, required::matches, ExtractionCountMode.UPTO,
					required.stack.getCount(), true)
				.getCount();

			if (amountFound < required.stack.getCount())
				continue;

			success = true;
			break;
		}

		if (!simulate && success) {
			amountFound = 0;
			for (ResourceHandler<ItemResource> cap : attachedInventories) {
				if (cap == null)
					cap = EmptyResourceHandler.instance();
				amountFound += ItemHelper
					.extract(cap, required::matches, ExtractionCountMode.UPTO,
						required.stack.getCount(), false)
					.getCount();
				if (amountFound < required.stack.getCount())
					continue;
				break;
			}
		}

		return success;
	}

	public void finishedPrinting() {
		if (replaceMode == ConfigureSchematicannonPacket.Option.REPLACE_EMPTY.ordinal())
			printer.sendBlockUpdates(level);
		inventory.set(0, ItemResource.EMPTY, 0);
		inventory.set(1, ItemResource.of(new ItemStack(AllItems.EMPTY_SCHEMATIC.get(), ItemUtil.getStack(inventory, 1)
			.getCount() + 1)), new ItemStack(AllItems.EMPTY_SCHEMATIC.get(), ItemUtil.getStack(inventory, 1)
			.getCount() + 1).getCount());
		state = State.STOPPED;
		statusMsg = "finished";
		resetPrinter();
		AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, worldPosition);
		sendUpdate = true;
	}

	protected void resetPrinter() {
		printer.resetSchematic();
		missingItem = null;
		sendUpdate = true;
		schematicProgress = 0;
		blocksPlaced = 0;
		blocksToPlace = 0;
	}

	protected boolean shouldPlace(BlockPos pos, BlockState state, BlockEntity be, BlockState toReplace,
		BlockState toReplaceOther, boolean isNormalCube) {
		if (pos.closerThan(getBlockPos(), 2f))
			return false;
		if (!replaceBlockEntities
			&& (toReplace.hasBlockEntity() || (toReplaceOther != null && toReplaceOther.hasBlockEntity())))
			return false;

		if (shouldIgnoreBlockState(state, be))
			return false;

		boolean placingAir = state.isAir();

		if (replaceMode == 3)
			return true;
		if (replaceMode == 2 && !placingAir)
			return true;
		if (replaceMode == 1 && (isNormalCube || (!toReplace.isRedstoneConductor(level, pos)
			&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos)))) && !placingAir)
			return true;
		if (replaceMode == 0 && !toReplace.isRedstoneConductor(level, pos)
			&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos)) && !placingAir)
			return true;

		return false;
	}

	protected boolean shouldIgnoreBlockState(BlockState state, BlockEntity be) {
		// Block doesn't have a mapping (Water, lava, etc)
		if (state.getBlock() == Blocks.STRUCTURE_VOID)
			return true;

		ItemRequirement requirement = ItemRequirement.of(state, be);
		if (requirement.isEmpty())
			return false;
		if (requirement.isInvalid())
			return false;

		// Block doesn't need to be placed twice (Doors, beds, double plants)
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
			&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
			return true;
		if (state.hasProperty(BlockStateProperties.BED_PART)
			&& state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD)
			return true;
		if (state.getBlock() instanceof PistonHeadBlock)
			return true;
		if (AllBlocks.BELT.has(state))
			return state.getValue(BeltBlock.PART) == BeltPart.MIDDLE;

		return false;
	}

	protected void tickFlyingBlocks() {
		List<LaunchedItem> toRemove = new LinkedList<>();
		for (LaunchedItem b : flyingBlocks)
			if (b.update(level))
				toRemove.add(b);
		flyingBlocks.removeAll(toRemove);
	}

	protected void refillFuelIfPossible() {
		if (hasCreativeCrate)
			return;
		if (remainingFuel > getShotsPerGunpowder()) {
			remainingFuel = getShotsPerGunpowder();
			sendUpdate = true;
			return;
		}

		if (remainingFuel > 0)
			return;

		// A resource handler hands back a copy, so consuming a unit has to go through extract
		// rather than shrinking the returned stack in place.
		if (!ItemUtil.getStack(inventory, 4)
			.isEmpty())
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred3Resource = inventory.getResource(4);
				int transferred3 = transferred3Resource.isEmpty() ? 0 : inventory.extract(4, transferred3Resource, 1, transaction);
				transaction.commit();
			}
		else {
			boolean externalGunpowderFound = false;
			for (ResourceHandler<ItemResource> cap : attachedInventories) {
				ResourceHandler<ItemResource> itemHandler = cap;

				if (itemHandler == null)
					itemHandler = EmptyResourceHandler.instance();

				if (ItemHelper.extract(itemHandler, stack -> inventory.isValid(4, ItemResource.of(stack)), 1, false)
					.isEmpty())
					continue;
				externalGunpowderFound = true;
				break;
			}
			if (!externalGunpowderFound)
				return;
		}

		remainingFuel += getShotsPerGunpowder();
		if (statusMsg.equals("noGunpowder")) {
			if (blocksPlaced > 0)
				state = State.RUNNING;
			statusMsg = "ready";
		}
		sendUpdate = true;
	}

	protected void tickPaperPrinter() {
		int BookInput = 2;
		int BookOutput = 3;

		ItemStack blueprint = ItemUtil.getStack(inventory, 0);
		ItemStack paper;
		try (Transaction transaction = Transaction.openRoot()) {
			ItemResource transferred3Resource = inventory.getResource(BookInput);
			int transferred3 = transferred3Resource.isEmpty() ? 0 : inventory.extract(BookInput, transferred3Resource, 1, transaction);
			paper = transferred3 <= 0 ? ItemStack.EMPTY : transferred3Resource.toStack(transferred3);
		}
		boolean outputFull = ItemUtil.getStack(inventory, BookOutput)
			.getCount() == inventory.getCapacityAsInt(BookOutput, ItemResource.EMPTY);

		if (printer.isErrored())
			return;

		if (!printer.isLoaded()) {
			if (!blueprint.isEmpty())
				initializePrinter(blueprint);
			return;
		}

		if (paper.isEmpty() || outputFull) {
			if (bookPrintingProgress != 0)
				sendUpdate = true;
			bookPrintingProgress = 0;
			dontUpdateChecklist = false;
			return;
		}

		if (bookPrintingProgress >= 1) {
			bookPrintingProgress = 0;

			if (!dontUpdateChecklist)
				updateChecklist();

			dontUpdateChecklist = true;
			ItemStack extractItem;
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred4Resource = inventory.getResource(BookInput);
				int transferred4 = transferred4Resource.isEmpty() ? 0 : inventory.extract(BookInput, transferred4Resource, 1, transaction);
				extractItem = transferred4 <= 0 ? ItemStack.EMPTY : transferred4Resource.toStack(transferred4);
				transaction.commit();
			}
			ItemStack stack = AllBlocks.CLIPBOARD.isIn(extractItem) ? checklist.createWrittenClipboard()
				: checklist.createWrittenBook();
			stack.setCount(ItemUtil.getStack(inventory, BookOutput)
				.getCount() + 1);
			inventory.set(BookOutput, ItemResource.of(stack), stack.getCount());
			sendUpdate = true;
			return;
		}

		bookPrintingProgress += 0.05f;
		sendUpdate = true;
	}

	public static BlockState stripBeltIfNotLast(BlockState blockState) {
		BeltPart part = blockState.getValue(BeltBlock.PART);
		if (part == BeltPart.MIDDLE)
			return Blocks.AIR.defaultBlockState();

		// is highest belt?
		boolean isLastSegment = false;
		Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope = blockState.getValue(BeltBlock.SLOPE);
		boolean positive = facing.getAxisDirection() == AxisDirection.POSITIVE;
		boolean start = part == BeltPart.START;
		boolean end = part == BeltPart.END;

		switch (slope) {
		case DOWNWARD:
			isLastSegment = start;
			break;
		case UPWARD:
			isLastSegment = end;
			break;
		default:
			isLastSegment = positive && end || !positive && start;
		}
		if (isLastSegment)
			return blockState;

		return AllBlocks.SHAFT.getDefaultState()
			.setValue(AbstractSimpleShaftBlock.AXIS, slope == BeltSlope.SIDEWAYS ? Axis.Y
				: facing.getClockWise()
					.getAxis());
	}

	protected void launchBlockOrBelt(BlockPos target, ItemStack icon, BlockState blockState, BlockEntity blockEntity) {
		if (AllBlocks.BELT.has(blockState)) {
			blockState = stripBeltIfNotLast(blockState);
			if (blockEntity instanceof BeltBlockEntity bbe && AllBlocks.BELT.has(blockState)) {
				CasingType[] casings = new CasingType[bbe.beltLength];
				Arrays.fill(casings, CasingType.NONE);
				BlockPos currentPos = target;
				for (int i = 0; i < bbe.beltLength; i++) {
					BlockState currentState = bbe.getLevel()
						.getBlockState(currentPos);
					if (!(currentState.getBlock() instanceof BeltBlock))
						break;
					if (!(bbe.getLevel()
						.getBlockEntity(currentPos) instanceof BeltBlockEntity beltAtSegment))
						break;
					casings[i] = beltAtSegment.casing;
					currentPos = BeltBlock.nextSegmentPosition(currentState, currentPos,
						blockState.getValue(BeltBlock.PART) != BeltPart.END);
				}
				launchBelt(target, blockState, bbe.beltLength, casings);
			} else if (blockState != Blocks.AIR.defaultBlockState())
				launchBlock(target, icon, blockState, null);
			return;
		}

		CompoundTag data = BlockHelper.prepareBlockEntityData(level, blockState, blockEntity);
		launchBlock(target, icon, blockState, data);
	}

	protected void launchBelt(BlockPos target, BlockState state, int length, CasingType[] casings) {
		blocksPlaced++;
		ItemStack connector = AllItems.BELT_CONNECTOR.asStack();
		flyingBlocks.add(new LaunchedItem.ForBelt(this.getBlockPos(), target, connector, state, casings));
		playFiringSound();
	}

	protected void launchBlock(BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data) {
		if (!state.isAir())
			blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForBlockState(this.getBlockPos(), target, stack, state, data));
		playFiringSound();
	}

	protected void launchEntity(BlockPos target, ItemStack stack, Entity entity) {
		blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForEntity(this.getBlockPos(), target, stack, entity));
		playFiringSound();
	}

	public void playFiringSound() {
		AllSoundEvents.SCHEMATICANNON_LAUNCH_BLOCK.playOnServer(level, worldPosition);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return SchematicannonMenu.create(id, inv, this);
	}

	@Override
	public Component getDisplayName() {
		return CreateLang.translateDirect("gui.schematicannon.title");
	}

	public void updateChecklist() {
		checklist.required.clear();
		checklist.damageRequired.clear();
		checklist.blocksNotLoaded = false;

		if (printer.isLoaded() && !printer.isErrored()) {
			blocksToPlace = blocksPlaced;
			blocksToPlace += printer.markAllBlockRequirements(checklist, level, this::shouldPlace);
			printer.markAllEntityRequirements(checklist);
		}

		checklist.gathered.clear();
		findInventories();
		for (ResourceHandler<ItemResource> cap : attachedInventories) {
			if (cap == null)
				continue;
			for (int slot = 0; slot < cap.size(); slot++) {
				ItemStack stackInSlot = ItemUtil.getStack(cap, slot);
				boolean anythingToTake;

				try (Transaction transaction = Transaction.openRoot()) {
					// never committed: this only asks whether the slot would give anything up
					anythingToTake = cap.extract(slot, cap.getResource(slot), 1, transaction) > 0;
				}

				if (!anythingToTake)
					continue;
				checklist.collect(stackInSlot);
			}
		}
		sendUpdate = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void lazyTick() {
		super.lazyTick();
		findInventories();
	}

	@Override
	public AABB getRenderBoundingBox() {
		return AABB.INFINITE;
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter componentInput) {
		SchematicannonOptions options = componentInput.getOrDefault(AllDataComponents.SCHEMATICANNON_OPTIONS,
				new SchematicannonOptions(2, true, false));
		replaceMode = options.replaceMode;
		skipMissing = options.skipMissing;
		replaceBlockEntities = options.replaceBlockEntities;
	}

	@Override
	protected void collectImplicitComponents(Builder components) {
		components.set(AllDataComponents.SCHEMATICANNON_OPTIONS,
			new SchematicannonOptions(replaceMode, skipMissing, replaceBlockEntities));
	}

	public enum State {
		STOPPED, PAUSED, RUNNING;
	}

	public record SchematicannonOptions(int replaceMode, boolean skipMissing, boolean replaceBlockEntities) {
		public static final Codec<SchematicannonOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("replace_mode").forGetter(SchematicannonOptions::replaceMode),
				Codec.BOOL.fieldOf("skip_missing").forGetter(SchematicannonOptions::skipMissing),
				Codec.BOOL.fieldOf("replace_block_entities").forGetter(SchematicannonOptions::replaceBlockEntities)
		).apply(i, SchematicannonOptions::new));

		public static final StreamCodec<ByteBuf, SchematicannonOptions> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, SchematicannonOptions::replaceMode,
				ByteBufCodecs.BOOL, SchematicannonOptions::skipMissing,
				ByteBufCodecs.BOOL, SchematicannonOptions::replaceBlockEntities,
				SchematicannonOptions::new
		);
	}

	@Override
	public void destroy() {
		super.destroy();
		if (level != null)
			ItemHelper.dropContents(level, worldPosition, inventory);
	}
}
