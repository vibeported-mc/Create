package com.simibubi.create.content.kinetics.millstone;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import com.simibubi.create.foundation.utility.NbtValueIO;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.simibubi.create.foundation.item.CombinedItemHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
public class MillstoneBlockEntity extends KineticBlockEntity implements Clearable {
	public ItemStacksResourceHandler inputInv;
	public ItemStacksResourceHandler outputInv;
	public ResourceHandler<ItemResource> capability;
	public int timer;
	private MillingRecipe lastRecipe;

	public MillstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inputInv = new ItemStacksResourceHandler(1);
		outputInv = new ItemStacksResourceHandler(9);
		capability = new MillstoneInventoryHandler();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Item.BLOCK,
				AllBlockEntityTypes.MILLSTONE.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this));
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.MILLSTONE);
	}

	@Override
	public void tickAudio() {
		super.tickAudio();

		if (getSpeed() == 0)
			return;
		if (ItemUtil.getStack(inputInv, 0)
			.isEmpty())
			return;

		float pitch = Mth.clamp((Math.abs(getSpeed()) / 256f) + .45f, .85f, 1f);
		SoundScapes.play(AmbienceGroup.MILLING, worldPosition, pitch);
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0)
			return;
		for (int i = 0; i < outputInv.size(); i++)
			if (ItemUtil.getStack(outputInv, i)
				.getCount() == outputInv.getCapacityAsInt(i, ItemResource.EMPTY))
				return;

		if (timer > 0) {
			timer -= getProcessingSpeed();

			if (level.isClientSide()) {
				spawnParticles();
				return;
			}
			if (timer <= 0)
				process();
			return;
		}

		if (ItemUtil.getStack(inputInv, 0)
			.isEmpty())
			return;

		RecipeWrapper inventoryIn = new RecipeWrapper(IItemHandler.of(inputInv));
		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
			if (!recipe.isPresent()) {
				timer = 100;
				sendData();
			} else {
				lastRecipe = recipe.get().value();
				timer = lastRecipe.getProcessingDuration();
				sendData();
			}
			return;
		}

		timer = lastRecipe.getProcessingDuration();
		sendData();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

	@Override
	public void clearContent() {
		emptyOut(inputInv);
		emptyOut(outputInv);
	}

	/** Empties every slot, the way clearing the backing list used to. */
	private static void emptyOut(ItemStacksResourceHandler inv) {
		for (int slot = 0; slot < inv.size(); slot++)
			inv.set(slot, ItemResource.EMPTY, 0);
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inputInv);
		ItemHelper.dropContents(level, worldPosition, outputInv);
	}

	private void process() {
		RecipeWrapper inventoryIn = new RecipeWrapper(IItemHandler.of(inputInv));

		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
			if (recipe.isEmpty())
				return;
			lastRecipe = recipe.get().value();
		}

		ItemStack stackInSlot = ItemUtil.getStack(inputInv, 0);
		ItemStack craftingRemainingItem = ItemHelper.getCraftingRemainder(stackInSlot);
		stackInSlot.shrink(1);
		inputInv.set(0, ItemResource.of(stackInSlot), stackInSlot.getCount());
		try (Transaction transaction = Transaction.openRoot()) {
			for (ItemStack rolled : lastRecipe.rollResults(level.getRandom()))
				if (!rolled.isEmpty())
					ResourceHandlerUtil.insertStacking(outputInv, ItemResource.of(rolled), rolled.getCount(), transaction);
			transaction.commit();
		}
		if (!craftingRemainingItem.isEmpty()) {
			try (Transaction transaction = Transaction.openRoot()) {
				int transferred = craftingRemainingItem.isEmpty() ? 0 : ResourceHandlerUtil.insertStacking(outputInv, ItemResource.of(craftingRemainingItem), craftingRemainingItem.getCount(), transaction);
				transaction.commit();
			}
		}
		award(AllAdvancements.MILLSTONE);

		sendData();
		setChanged();
	}

	public void spawnParticles() {
		ItemStack stackInSlot = ItemUtil.getStack(inputInv, 0);
		if (stackInSlot.isEmpty())
			return;

		ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromStack(stackInSlot));
		float angle = level.getRandom().nextFloat() * 360;
		Vec3 offset = new Vec3(0, 0, 0.5f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y);

		Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
		target = VecHelper.offsetRandomly(target.subtract(offset), level.getRandom(), 1 / 128f);
		level.addParticle(data, center.x, center.y, center.z, target.x, target.y, target.z);
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("Timer", timer);
		compound.put("InputInventory", NbtValueIO.serialize(inputInv, registries));
		compound.put("OutputInventory", NbtValueIO.serialize(outputInv, registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		timer = compound.getIntOr("Timer", 0);
		NbtValueIO.deserialize(inputInv, compound.getCompoundOrEmpty("InputInventory"), registries);
		NbtValueIO.deserialize(outputInv, compound.getCompoundOrEmpty("OutputInventory"), registries);
		super.read(compound, registries, clientPacket);
	}

	public int getProcessingSpeed() {
		return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
	}

	private boolean canProcess(ItemStack stack) {
		ItemStacksResourceHandler tester = new ItemStacksResourceHandler(1);
		tester.set(0, ItemResource.of(stack), stack.getCount());
		RecipeWrapper inventoryIn = new RecipeWrapper(IItemHandler.of(tester));

		if (lastRecipe != null && lastRecipe.matches(inventoryIn, level))
			return true;
		return AllRecipeTypes.MILLING.find(inventoryIn, level)
			.isPresent();
	}

	/**
	 * Input and output as one handler: the input half only accepts what the millstone can mill, and
	 * the output half is extract-only.
	 */
	private class MillstoneInventoryHandler extends CombinedItemHandler {

		public MillstoneInventoryHandler() {
			super(inputInv, outputInv);
		}

		private boolean isOutput(int index) {
			return getHandlerFromIndex(getHandlerIndex(index)) == outputInv;
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (isOutput(index))
				return false;
			return canProcess(resource.toStack(1)) && super.isValid(index, resource);
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!isValid(index, resource))
				return 0;
			return super.insert(index, resource, amount, transaction);
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!isOutput(index))
				return 0;
			return super.extract(index, resource, amount, transaction);
		}

	}
}
