package com.simibubi.create.content.equipment.blueprint;

import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import com.simibubi.create.foundation.utility.IInteractionChecker;

import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
public class BlueprintEntity extends HangingEntity
	implements IEntityWithComplexSpawn, SpecialEntityItemRequirement, ISyncPersistentData, IInteractionChecker {

	protected int size;
	protected Direction verticalOrientation;

	/**
	 * Blueprints hang on all six faces, which {@link HangingEntity#setDirection} rejects, so the
	 * facing is kept here and handed back through {@link #getDirection()}.
	 */
	protected Direction direction;

	@SuppressWarnings("unchecked")
	public BlueprintEntity(EntityType<?> p_i50221_1_, Level p_i50221_2_) {
		super((EntityType<? extends HangingEntity>) p_i50221_1_, p_i50221_2_);
		size = 1;
	}

	public BlueprintEntity(Level world, BlockPos pos, Direction facing, Direction verticalOrientation) {
		super(AllEntityTypes.CRAFTING_BLUEPRINT.get(), world, pos);

		for (int size = 3; size > 0; size--) {
			this.size = size;
			this.updateFacingWithBoundingBox(facing, verticalOrientation);
			if (this.survives())
				break;
		}
	}

	public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
		@SuppressWarnings("unchecked")
		EntityType.Builder<BlueprintEntity> entityBuilder = (EntityType.Builder<BlueprintEntity>) builder;
		return entityBuilder;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		CompoundTag p_213281_1_ = new CompoundTag();
		p_213281_1_.putByte("Facing", (byte) this.direction.get3DDataValue());
		p_213281_1_.putByte("Orientation", (byte) this.verticalOrientation.get3DDataValue());
		p_213281_1_.putInt("Size", size);
		NbtValueIO.store(output, p_213281_1_);
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		readFacing(NbtValueIO.read(input));
	}

	protected void readFacing(CompoundTag p_70037_1_) {
		if (p_70037_1_.contains("Facing")) {
			this.direction = Direction.from3DDataValue(p_70037_1_.getByteOr("Facing", (byte) 0));
			this.verticalOrientation = Direction.from3DDataValue(p_70037_1_.getByteOr("Orientation", (byte) 0));
			this.size = p_70037_1_.getIntOr("Size", 0);
		} else {
			this.direction = Direction.SOUTH;
			this.verticalOrientation = Direction.DOWN;
			this.size = 1;
		}
		this.updateFacingWithBoundingBox(this.direction, this.verticalOrientation);
	}

	protected void updateFacingWithBoundingBox(Direction facing, Direction verticalOrientation) {
		Objects.requireNonNull(facing);
		this.direction = facing;
		this.verticalOrientation = verticalOrientation;
		if (facing.getAxis()
			.isHorizontal()) {
			setXRot(0.0F);
			setYRot(this.direction.get2DDataValue() * 90);
		} else {
			setXRot(-90 * facing.getAxisDirection()
				.getStep());
			setYRot(verticalOrientation.getAxis()
				.isHorizontal() ? 180 + verticalOrientation.toYRot() : 0);
		}

		this.xRotO = getXRot();
		this.yRotO = getYRot();
		this.recalculateBoundingBox();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return super.getDimensions(pose).withEyeHeight(0);
	}

	@Override
	protected AABB calculateBoundingBox(BlockPos blockPos, Direction direction) {
		Vec3 pos = Vec3.atLowerCornerOf(getPos())
				.add(.5, .5, .5)
				.subtract(Vec3.atLowerCornerOf(direction.getUnitVec3i())
						.scale(0.46875));
		double d1 = pos.x;
		double d2 = pos.y;
		double d3 = pos.z;
		this.setPosRaw(d1, d2, d3);

		Axis axis = direction.getAxis();
		if (size == 2)
			pos = pos.add(Vec3.atLowerCornerOf(axis.isHorizontal() ? direction.getCounterClockWise()
									.getUnitVec3i()
									: verticalOrientation.getClockWise()
									.getUnitVec3i())
							.scale(0.5))
					.add(Vec3
							.atLowerCornerOf(axis.isHorizontal() ? Direction.UP.getUnitVec3i()
									: direction == Direction.UP ? verticalOrientation.getUnitVec3i()
									: verticalOrientation.getOpposite()
									.getUnitVec3i())
							.scale(0.5));

		d1 = pos.x;
		d2 = pos.y;
		d3 = pos.z;

		double d4 = (double) this.getWidth();
		double d5 = (double) this.getHeight();
		double d6 = (double) this.getWidth();
		Direction.Axis direction$axis = this.direction.getAxis();
		switch (direction$axis) {
			case X:
				d4 = 1.0D;
				break;
			case Y:
				d5 = 1.0D;
				break;
			case Z:
				d6 = 1.0D;
		}

		d4 = d4 / 32.0D;
		d5 = d5 / 32.0D;
		d6 = d6 / 32.0D;

		return new AABB(d1 - d4, d2 - d5, d3 - d6, d1 + d4, d2 + d5, d3 + d6);
	}

	@Override
	protected void recalculateBoundingBox() {
		if (this.direction != null && this.verticalOrientation != null) {
			setBoundingBox(calculateBoundingBox(pos, direction));
		}
	}
	@Override
	public void setPos(double pX, double pY, double pZ) {
		setPosRaw(pX, pY, pZ);
		super.setPos(pX, pY, pZ);
	}

	@Override
	public boolean survives() {
		if (!level().noCollision(this))
			return false;

		int i = Math.max(1, this.getWidth() / 16);
		int j = Math.max(1, this.getHeight() / 16);
		BlockPos blockpos = this.pos.relative(this.direction.getOpposite());
		Direction upDirection = direction.getAxis()
			.isHorizontal() ? Direction.UP
			: direction == Direction.UP ? verticalOrientation : verticalOrientation.getOpposite();
		Direction newDirection = direction.getAxis()
			.isVertical() ? verticalOrientation.getClockWise() : direction.getCounterClockWise();
		BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();

		for (int k = 0; k < i; ++k) {
			for (int l = 0; l < j; ++l) {
				int i1 = (i - 1) / -2;
				int j1 = (j - 1) / -2;
				blockpos$mutable.set(blockpos)
					.move(newDirection, k + i1)
					.move(upDirection, l + j1);
				BlockState blockstate = this.level().getBlockState(blockpos$mutable);
				if (Block.canSupportCenter(this.level(), blockpos$mutable, this.direction))
					continue;
				if (!blockstate.isSolid() && !DiodeBlock.isDiode(blockstate)) {
					return false;
				}
			}
		}

		return this.canCoexist(false);
	}

	public int getWidth() {
		return 16 * size;
	}

	public int getHeight() {
		return 16 * size;
	}

	@Override
	public boolean skipAttackInteraction(Entity source) {
		if (!(source instanceof Player player) || level().isClientSide())
			return super.skipAttackInteraction(source);

		double attrib = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + (player.isCreative() ? 0 : -0.5F);

		Vec3 eyePos = source.getEyePosition(1);
		Vec3 look = source.getViewVector(1);
		Vec3 target = eyePos.add(look.scale(attrib));

		Optional<Vec3> rayTrace = getBoundingBox().clip(eyePos, target);
		if (!rayTrace.isPresent())
			return super.skipAttackInteraction(source);

		Vec3 hitVec = rayTrace.get();
		BlueprintSection sectionAt = getSectionAt(hitVec.subtract(position()));
		ItemStacksResourceHandler items = sectionAt.getItems();

		if (ItemHandlerHelpers.getStackInSlot(items, 9)
			.isEmpty())
			return super.skipAttackInteraction(source);
		for (int i = 0; i < items.size(); i++)
			ItemHandlerHelpers.setStackInSlot(items, i, ItemStack.EMPTY);
		sectionAt.save(items);
		return true;
	}

	@Override
	public void dropItem(ServerLevel serverLevel, @Nullable Entity p_110128_1_) {
		if (!serverLevel.getGameRules()
			.get(GameRules.ENTITY_DROPS))
			return;

		playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
		if (p_110128_1_ instanceof Player playerentity) {
			if (playerentity.getAbilities().instabuild)
				return;
		}

		spawnAtLocation(serverLevel, AllItems.CRAFTING_BLUEPRINT.asStack());
	}

	@Override
	public ItemStack getPickResult() {
		return AllItems.CRAFTING_BLUEPRINT.asStack();
	}

	@Override
	public ItemRequirement getRequiredItems() {
		return new ItemRequirement(ItemUseType.CONSUME, AllItems.CRAFTING_BLUEPRINT.get());
	}

	@Override
	public void playPlacementSound() {
		this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
	}

	@Override
	public void snapTo(double p_70012_1_, double p_70012_3_, double p_70012_5_, float p_70012_7_, float p_70012_8_) {
		this.setPos(p_70012_1_, p_70012_3_, p_70012_5_);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
		CompoundTag compound = new CompoundTag();
		compound.putByte("Facing", (byte) this.direction.get3DDataValue());
		compound.putByte("Orientation", (byte) this.verticalOrientation.get3DDataValue());
		compound.putInt("Size", size);
		registryFriendlyByteBuf.writeNbt(compound);
		registryFriendlyByteBuf.writeNbt(getPersistentData());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
		readFacing(registryFriendlyByteBuf.readNbt());
		getPersistentData().merge(registryFriendlyByteBuf.readNbt());
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 vec) {
		if (player instanceof FakePlayer)
			return InteractionResult.PASS;

		boolean holdingWrench = AllItems.WRENCH.isIn(player.getItemInHand(hand));
		BlueprintSection section = getSectionAt(vec);
		ItemStacksResourceHandler items = section.getItems();

		if (!holdingWrench && !level().isClientSide() && !ItemHandlerHelpers.getStackInSlot(items, 9)
			.isEmpty()) {

			ResourceHandler<ItemResource> playerInv = PlayerInventoryWrapper.of(player.getInventory());
			boolean firstPass = true;
			int amountCrafted = 0;
			CommonHooks.setCraftingPlayer(player);
			Optional<RecipeHolder<CraftingRecipe>> recipe = Optional.empty();

			do {
				Map<Integer, ItemStack> stacksTaken = new HashMap<>();
				Map<Integer, ItemStack> craftingGrid = new HashMap<>();
				boolean success = true;

				Search:
				for (int i = 0; i < 9; i++) {
					FilterItemStack requestedItem = FilterItemStack.of(ItemHandlerHelpers.getStackInSlot(items, i));
					if (requestedItem.isEmpty()) {
						craftingGrid.put(i, ItemStack.EMPTY);
						continue;
					}

					for (int slot = 0; slot < playerInv.size(); slot++) {
						if (!requestedItem.test(level(), ItemHandlerHelpers.getStackInSlot(playerInv, slot)))
							continue;
						ItemStack currentItem = ItemHandlerHelpers.extractItem(playerInv, slot, 1, false);
						if (stacksTaken.containsKey(slot))
							stacksTaken.get(slot)
								.grow(1);
						else
							stacksTaken.put(slot, currentItem.copy());
						craftingGrid.put(i, currentItem);
						continue Search;
					}

					success = false;
					break;
				}

				if (success) {
					CraftingContainer craftingInventory = new BlueprintCraftingInventory(craftingGrid);

					if (!recipe.isPresent())
						recipe = ((ServerLevel) level()).recipeAccess()
							.getRecipeFor(RecipeType.CRAFTING, craftingInventory.asCraftInput(), level());
					ItemStack result = recipe.filter(r -> r.value().matches(craftingInventory.asCraftInput(), level()))
						.map(r -> r.value().assemble(craftingInventory.asCraftInput()))
						.orElse(ItemStack.EMPTY);

					if (result.isEmpty()) {
						success = false;
					} else if (result.getCount() + amountCrafted > 64) {
						success = false;
					} else {
						amountCrafted += result.getCount();
						result.onCraftedBy(player, 1);
						EventHooks.firePlayerCraftingEvent(player, result, craftingInventory);
						NonNullList<ItemStack> nonnulllist = recipe
							.map(r -> r.value().getRemainingItems(craftingInventory.asCraftInput()))
							.orElseGet(() -> NonNullList.withSize(9, ItemStack.EMPTY));

						if (firstPass)
							level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
								.2f, 1f + level().getRandom().nextFloat());
						player.getInventory()
							.placeItemBackInInventory(result);
						for (ItemStack itemStack : nonnulllist)
							player.getInventory()
								.placeItemBackInInventory(itemStack);
						firstPass = false;
					}
				}

				if (!success) {
					for (Entry<Integer, ItemStack> entry : stacksTaken.entrySet())
						ItemHandlerHelpers.insertItem(playerInv, entry.getKey(), entry.getValue(), false);
					break;
				}

			} while (player.isShiftKeyDown());
			CommonHooks.setCraftingPlayer(null);
			return InteractionResult.SUCCESS;
		}

		int i = section.index;
		if (!level().isClientSide() && player instanceof ServerPlayer) {
			player.openMenu(section, buf -> {
				buf.writeVarInt(getId());
				buf.writeVarInt(i);
			});
		}

		return InteractionResult.SUCCESS;
	}

	public BlueprintSection getSectionAt(Vec3 vec) {
		int index = 0;
		if (size > 1) {
			vec = VecHelper.rotate(vec, getYRot(), Axis.Y);
			vec = VecHelper.rotate(vec, -getXRot(), Axis.X);
			vec = vec.add(0.5, 0.5, 0);
			if (size == 3)
				vec = vec.add(1, 1, 0);
			int x = Mth.clamp(Mth.floor(vec.x), 0, size - 1);
			int y = Mth.clamp(Mth.floor(vec.y), 0, size - 1);
			index = x + y * size;
		}

		BlueprintSection section = getSection(index);
		return section;
	}

	static class BlueprintCraftingInventory extends TransientCraftingContainer {

		private static final AbstractContainerMenu dummyContainer = new AbstractContainerMenu(null, -1) {
			public boolean stillValid(Player playerIn) {
				return false;
			}

			@Override
			public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
				return ItemStack.EMPTY;
			}
		};

		public BlueprintCraftingInventory(Map<Integer, ItemStack> items) {
			super(dummyContainer, 3, 3);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					ItemStack stack = items.get(y * 3 + x);
					setItem(y * 3 + x, stack == null ? ItemStack.EMPTY : stack.copy());
				}
			}
		}

	}

	public CompoundTag getOrCreateRecipeCompound() {
		CompoundTag persistentData = getPersistentData();
		if (!persistentData.contains("Recipes"))
			persistentData.put("Recipes", new CompoundTag());
		return persistentData.getCompoundOrEmpty("Recipes");
	}

	private Map<Integer, BlueprintSection> sectionCache = new HashMap<>();

	public BlueprintSection getSection(int index) {
		return sectionCache.computeIfAbsent(index, i -> new BlueprintSection(i));
	}

	class BlueprintSection implements MenuProvider, IInteractionChecker {
		int index;
		Couple<ItemStack> cachedDisplayItems;
		public boolean inferredIcon = false;

		public BlueprintSection(int index) {
			this.index = index;
		}

		public Couple<ItemStack> getDisplayItems() {
			if (cachedDisplayItems != null)
				return cachedDisplayItems;
			ItemStacksResourceHandler items = getItems();
			return cachedDisplayItems = Couple.create(ItemHandlerHelpers.getStackInSlot(items, 9), ItemHandlerHelpers.getStackInSlot(items, 10));
		}

		public ItemStacksResourceHandler getItems() {
			ItemStacksResourceHandler newInv = new ItemStacksResourceHandler(11);
			CompoundTag list = getOrCreateRecipeCompound();
			CompoundTag invNBT = list.getCompoundOrEmpty(index + "");
			inferredIcon = list.getBooleanOr("InferredIcon", false);
			if (!invNBT.isEmpty())
				ItemHandlerHelpers.deserializeNBT(newInv, registryAccess(), invNBT);
			return newInv;
		}

		public void save(ItemStacksResourceHandler inventory) {
			CompoundTag list = getOrCreateRecipeCompound();
			list.put(index + "", ItemHandlerHelpers.serializeNBT(inventory, registryAccess()));
			list.putBoolean("InferredIcon", inferredIcon);
			cachedDisplayItems = null;
			if (!level().isClientSide())
				syncPersistentDataWithTracking(BlueprintEntity.this);
		}

		public boolean isEntityAlive() {
			return isAlive();
		}

		public Level getBlueprintWorld() {
			return level();
		}

		@Override
		public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
			return BlueprintMenu.create(id, inv, this);
		}

		@Override
		public Component getDisplayName() {
			return AllItems.CRAFTING_BLUEPRINT.asStack()
				.getHoverName();
		}

		@Override
		public boolean canPlayerUse(Player player) {
			return BlueprintEntity.this.canPlayerUse(player);
		}

	}

	@Override
	public void onPersistentDataUpdated() {
		sectionCache.clear();
	}

	@Override
	public boolean canPlayerUse(Player player) {
		return player.isWithinEntityInteractionRange(this, 8);
	}

}
