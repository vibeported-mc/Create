package com.simibubi.create.foundation.fluid;

import net.neoforged.neoforge.fluids.crafting.display.FluidStackContentsFactory;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.util.context.ContextMap;
import java.util.List;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.createmod.catnip.api.data.Pair;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
public class FluidHelper {

	public static enum FluidExchange {
		ITEM_TO_TANK, TANK_TO_ITEM;
	}

	public static boolean isWater(Fluid fluid) {
		return convertToStill(fluid) == Fluids.WATER;
	}

	public static boolean isLava(Fluid fluid) {
		return convertToStill(fluid) == Fluids.LAVA;
	}

	/**
	 * The fluid stacks an ingredient accepts, each with the ingredient's amount and whatever components
	 * the ingredient asks for.
	 *
	 * <p>1.21.1's {@code SizedFluidIngredient#getFluids} gave whole stacks. 26.2's
	 * {@code FluidIngredient#fluids} gives the fluids alone, so a stack built from one loses its
	 * components: every potion a recipe wanted showed as an uncraftable potion. The stacks, components
	 * included, are in the ingredient's display, which is resolved here. A display that resolves to
	 * nothing without registries -- a tag, when there are none to give -- falls back to the bare fluids.
	 */
	public static List<FluidStack> matchingStacks(SizedFluidIngredient ingredient, @Nullable HolderLookup.Provider registries) {
		ContextMap.Builder context = new ContextMap.Builder();
		if (registries != null)
			context.withParameter(SlotDisplayContext.REGISTRIES, registries);
		List<FluidStack> stacks = ingredient.ingredient()
			.display()
			.resolve(context.create(SlotDisplayContext.CONTEXT), FluidStackContentsFactory.INSTANCE)
			.filter(stack -> !stack.isEmpty())
			.map(stack -> stack.copyWithAmount(ingredient.amount()))
			.toList();
		if (!stacks.isEmpty())
			return stacks;
		return ingredient.ingredient()
			.fluids()
			.stream()
			.map(fluid -> new FluidStack(fluid, ingredient.amount()))
			.toList();
	}

	public static boolean isSame(FluidStack fluidStack, FluidStack fluidStack2) {
		return fluidStack.getFluid() == fluidStack2.getFluid();
	}

	public static boolean isSame(FluidStack fluidStack, Fluid fluid) {
		return fluidStack.getFluid() == fluid;
	}

	@SuppressWarnings("deprecation")
	public static boolean isTag(Fluid fluid, TagKey<Fluid> tag) {
		return fluid.is(tag);
	}

	public static boolean isTag(FluidState fluid, TagKey<Fluid> tag) {
		return fluid.is(tag);
	}

	public static boolean isTag(FluidStack fluid, TagKey<Fluid> tag) {
		return isTag(fluid.getFluid(), tag);
	}

	public static SoundEvent getFillSound(FluidStack fluid) {
		SoundEvent soundevent = fluid.getFluid()
			.getFluidType()
			.getSound(fluid, SoundActions.BUCKET_FILL);
		if (soundevent == null)
			soundevent =
				FluidHelper.isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
		return soundevent;
	}

	public static SoundEvent getEmptySound(FluidStack fluid) {
		SoundEvent soundevent = fluid.getFluid()
			.getFluidType()
			.getSound(fluid, SoundActions.BUCKET_EMPTY);
		if (soundevent == null)
			soundevent =
				FluidHelper.isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
		return soundevent;
	}

	public static boolean hasBlockState(Fluid fluid) {
		BlockState blockState = fluid.defaultFluidState()
			.createLegacyBlock();
		return blockState != null && blockState != Blocks.AIR.defaultBlockState();
	}

	public static FluidStack copyStackWithAmount(FluidStack fs, int amount) {
		if (amount <= 0)
			return FluidStack.EMPTY;
		if (fs.isEmpty())
			return FluidStack.EMPTY;
		FluidStack copy = fs.copy();
		copy.setAmount(amount);
		return copy;
	}

	public static Fluid convertToFlowing(Fluid fluid) {
		if (fluid == Fluids.WATER)
			return Fluids.FLOWING_WATER;
		if (fluid == Fluids.LAVA)
			return Fluids.FLOWING_LAVA;
		if (fluid instanceof BaseFlowingFluid)
			return ((BaseFlowingFluid) fluid).getFlowing();
		return fluid;
	}

	public static Fluid convertToStill(Fluid fluid) {
		if (fluid == Fluids.FLOWING_WATER)
			return Fluids.WATER;
		if (fluid == Fluids.FLOWING_LAVA)
			return Fluids.LAVA;
		if (fluid instanceof BaseFlowingFluid)
			return ((BaseFlowingFluid) fluid).getSource();
		return fluid;
	}

	public static boolean tryEmptyItemIntoBE(Level worldIn, Player player, InteractionHand handIn, ItemStack heldItem,
		SmartBlockEntity be) {
		if (!GenericItemEmptying.canItemBeEmptied(worldIn, heldItem))
			return false;

		Pair<FluidStack, ItemStack> emptyingResult = GenericItemEmptying.emptyItem(worldIn, heldItem, true);
		ResourceHandler<FluidResource> capability = worldIn.getCapability(Capabilities.Fluid.BLOCK, be.getBlockPos(), null);
		FluidStack fluidStack = emptyingResult.getFirst();

		if (capability == null)
			return false;

		try (Transaction transaction = Transaction.openRoot()) {
			// never committed: this only asks whether the block would take all of it
			if (capability.insert(FluidResource.of(fluidStack), fluidStack.getAmount(),
				transaction) != fluidStack.getAmount())
				return false;
		}
		if (worldIn.isClientSide())
			return true;

		ItemStack copyOfHeld = heldItem.copy();
		emptyingResult = GenericItemEmptying.emptyItem(worldIn, copyOfHeld, false);
		try (Transaction transaction = Transaction.openRoot()) {
			int transferred = fluidStack.isEmpty() ? 0 : capability.insert(FluidResource.of(fluidStack), fluidStack.getAmount(), transaction);
			transaction.commit();
		}

		if (!player.isCreative() && !(be instanceof CreativeFluidTankBlockEntity)) {
			if (copyOfHeld.isEmpty())
				player.setItemInHand(handIn, emptyingResult.getSecond());
			else {
				player.setItemInHand(handIn, copyOfHeld);
				player.getInventory()
					.placeItemBackInInventory(emptyingResult.getSecond());
			}
		}
		return true;
	}

	public static boolean tryFillItemFromBE(Level world, Player player, InteractionHand handIn, ItemStack heldItem,
		SmartBlockEntity be) {
		if (!GenericItemFilling.canItemBeFilled(world, heldItem))
			return false;

		ResourceHandler<FluidResource> capability = world.getCapability(Capabilities.Fluid.BLOCK, be.getBlockPos(), null);

		if (capability == null)
			return false;

		for (int i = 0; i < capability.size(); i++) {
			FluidStack fluid = FluidUtil.getStack(capability, i);
			if (fluid.isEmpty())
				continue;
			int requiredAmountForItem = GenericItemFilling.getRequiredAmountForItem(world, heldItem, fluid.copy());
			if (requiredAmountForItem == -1)
				continue;
			if (requiredAmountForItem > fluid.getAmount())
				continue;

			if (world.isClientSide())
				return true;

			if (player.isCreative() || be instanceof CreativeFluidTankBlockEntity)
				heldItem = heldItem.copy();
			ItemStack out = GenericItemFilling.fillItem(world, requiredAmountForItem, heldItem, fluid.copy());

			FluidStack copy = fluid.copy();
			copy.setAmount(requiredAmountForItem);
			try (Transaction transaction = Transaction.openRoot()) {
				if (!copy.isEmpty())
					capability.extract(FluidResource.of(copy), copy.getAmount(), transaction);
				transaction.commit();
			}

			if (!player.isCreative())
				player.getInventory()
					.placeItemBackInInventory(out);
			be.notifyUpdate();
			return true;
		}

		return false;
	}

	@Nullable
	public static FluidExchange exchange(ResourceHandler<FluidResource> fluidTank, ResourceHandler<FluidResource> fluidItem, FluidExchange preferred,
		int maxAmount) {
		return exchange(fluidTank, fluidItem, preferred, true, maxAmount);
	}

	@Nullable
	public static FluidExchange exchangeAll(ResourceHandler<FluidResource> fluidTank, ResourceHandler<FluidResource> fluidItem,
		FluidExchange preferred) {
		return exchange(fluidTank, fluidItem, preferred, false, Integer.MAX_VALUE);
	}

	@Nullable
	private static FluidExchange exchange(ResourceHandler<FluidResource> fluidTank, ResourceHandler<FluidResource> fluidItem, FluidExchange preferred,
		boolean singleOp, int maxTransferAmountPerTank) {

		// Locks in the transfer direction of this operation
		FluidExchange lockedExchange = null;

		for (int tankSlot = 0; tankSlot < fluidTank.size(); tankSlot++) {
			for (int slot = 0; slot < fluidItem.size(); slot++) {

				FluidStack fluidInTank = FluidUtil.getStack(fluidTank, tankSlot);
				int tankCapacity = fluidTank.getCapacityAsInt(tankSlot, FluidResource.EMPTY) - fluidInTank.getAmount();
				boolean tankEmpty = fluidInTank.isEmpty();

				FluidStack fluidInItem = FluidUtil.getStack(fluidItem, tankSlot);
				int itemCapacity = fluidItem.getCapacityAsInt(tankSlot, FluidResource.EMPTY) - fluidInItem.getAmount();
				boolean itemEmpty = fluidInItem.isEmpty();

				boolean undecided = lockedExchange == null;
				boolean canMoveToTank = (undecided || lockedExchange == FluidExchange.ITEM_TO_TANK) && tankCapacity > 0;
				boolean canMoveToItem = (undecided || lockedExchange == FluidExchange.TANK_TO_ITEM) && itemCapacity > 0;

				// Incompatible Liquids
				if (!tankEmpty && !itemEmpty && !FluidStack.isSameFluidSameComponents(fluidInItem, fluidInTank))
					continue;

				// Transfer liquid to tank
				if (((tankEmpty || itemCapacity <= 0) && canMoveToTank)
					|| undecided && preferred == FluidExchange.ITEM_TO_TANK) {

					int amount;

					try (Transaction transaction = Transaction.openRoot()) {
						ResourceStack<FluidResource> drained = ResourceHandlerUtil.extractFirst(fluidItem,
							resource -> true, Math.min(maxTransferAmountPerTank, tankCapacity), transaction);
						amount = drained == null ? 0
							: fluidTank.insert(drained.resource(), drained.amount(), transaction);
						transaction.commit();
					}
					if (amount > 0) {
						lockedExchange = FluidExchange.ITEM_TO_TANK;
						if (singleOp)
							return lockedExchange;
						continue;
					}
				}

				// Transfer liquid from tank
				if (((itemEmpty || tankCapacity <= 0) && canMoveToItem)
					|| undecided && preferred == FluidExchange.TANK_TO_ITEM) {

					int amount;

					try (Transaction transaction = Transaction.openRoot()) {
						ResourceStack<FluidResource> drained = ResourceHandlerUtil.extractFirst(fluidTank,
							resource -> true, Math.min(maxTransferAmountPerTank, itemCapacity), transaction);
						amount = drained == null ? 0
							: fluidItem.insert(drained.resource(), drained.amount(), transaction);
						transaction.commit();
					}
					if (amount > 0) {
						lockedExchange = FluidExchange.TANK_TO_ITEM;
						if (singleOp)
							return lockedExchange;
						continue;
					}

				}

			}
		}

		return null;
	}

	/**
	 * 26.2 dropped FluidStack's saveOptional/parseOptional pair in favour of its optional codec.
	 */
	public static Tag saveOptional(FluidStack stack, HolderLookup.Provider registries) {
		return CatnipCodecUtils.encode(FluidStack.OPTIONAL_CODEC, registries, stack)
			.orElseGet(CompoundTag::new);
	}

	public static FluidStack parseOptional(HolderLookup.Provider registries, Tag tag) {
		return CatnipCodecUtils.decode(FluidStack.OPTIONAL_CODEC, registries, tag)
			.orElse(FluidStack.EMPTY);
	}

}
