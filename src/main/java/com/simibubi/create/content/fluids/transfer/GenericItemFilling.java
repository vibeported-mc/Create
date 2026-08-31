package com.simibubi.create.content.fluids.transfer;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
public class GenericItemFilling {

	/**
	 * Checks if an ItemStack's ResourceHandler<FluidResource> is valid. Ideally, this check would
	 * not be necessary. Unfortunately, some mods that copy the functionality of the
	 * MilkBucketItem copy the FluidBucketWrapper capability that is patched in by
	 * Forge without looking into what it actually does. In all cases this is
	 * incorrect because having a non-bucket item turn into a bucket item does not
	 * make sense.
	 *
	 * <p>This check is only necessary for filling since a FluidBucketWrapper will be
	 * empty if it is initialized with a non-bucket item.
	 *
	 * @param stack The ItemStack.
	 * @param fluidHandler The ResourceHandler<FluidResource> instance retrieved from the ItemStack.
	 * @return If the ResourceHandler<FluidResource> is valid for the passed ItemStack.
	 */
	public static boolean isFluidHandlerValid(ItemStack stack, ResourceHandler<FluidResource> fluidHandler) {
		// Not instanceof in case a correct subclass is made
		if (fluidHandler.getClass() == BucketResourceHandler.class) {
			Item item = stack.getItem();
			// The bucket handler turns whatever it holds into a plain bucket, which is only right for one
			if (item.getClass() != BucketItem.class && item != Items.MILK_BUCKET) {
				return false;
			}
		}
		return true;
	}

	public static boolean canItemBeFilled(Level world, ItemStack stack) {
		if (stack.getItem() == Items.GLASS_BOTTLE)
			return true;
		if (stack.getItem() == Items.MILK_BUCKET)
			return false;

		ResourceHandler<FluidResource> capability = Capabilities.Fluid.ITEM.getCapability(stack, ItemAccess.forStack(stack));
		if (capability == null)
			return false;
		if (!isFluidHandlerValid(stack, capability))
			return false;
		for (int i = 0; i < capability.size(); i++) {
			if (FluidUtil.getStack(capability, i)
				.getAmount() < capability.getCapacityAsInt(i, FluidResource.EMPTY))
				return true;
		}
		return false;
	}

	public static int getRequiredAmountForItem(Level world, ItemStack stack, FluidStack availableFluid) {
		if (stack.getItem() == Items.GLASS_BOTTLE && canFillGlassBottleInternally(availableFluid))
			return PotionFluidHandler.getRequiredAmountForFilledBottle(stack, availableFluid);
		if (stack.getItem() == Items.BUCKET && canFillBucketInternally(availableFluid))
			return 1000;

		ResourceHandler<FluidResource> capability = Capabilities.Fluid.ITEM.getCapability(stack, ItemAccess.forStack(stack));
		if (capability == null)
			return -1;
		if (capability instanceof BucketResourceHandler) {
			Item filledBucket = availableFluid.getFluid()
				.getBucket();
			if (filledBucket == null || filledBucket == Items.AIR)
				return -1;
			if (!FluidUtil.getStack(capability, 0)
				.isEmpty())
				return -1;
			return 1000;
		}

		int filled;
		try (Transaction transaction = Transaction.openRoot()) {
			int transferred = availableFluid.isEmpty() ? 0 : capability.insert(FluidResource.of(availableFluid), availableFluid.getAmount(), transaction);
			filled = transferred;
		}
		return filled == 0 ? -1 : filled;
	}

	private static boolean canFillGlassBottleInternally(FluidStack availableFluid) {
		Fluid fluid = availableFluid.getFluid();
		if (fluid.isSame(Fluids.WATER))
			return true;
		if (fluid.isSame(AllFluids.POTION.get()))
			return true;
		if (fluid.isSame(AllFluids.TEA.get()))
			return true;
		return false;
	}

	private static boolean canFillBucketInternally(FluidStack availableFluid) {
		return false;
	}

	public static ItemStack fillItem(Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid) {
		FluidStack toFill = availableFluid.copy();
		toFill.setAmount(requiredAmount);
		availableFluid.shrink(requiredAmount);

		if (stack.getItem() == Items.GLASS_BOTTLE && canFillGlassBottleInternally(toFill)) {
			ItemStack fillBottle;
			Fluid fluid = toFill.getFluid();
			if (FluidHelper.isWater(fluid))
				fillBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
			else if (fluid.isSame(AllFluids.TEA.get()))
				fillBottle = AllItems.BUILDERS_TEA.asStack();
			else
				fillBottle = PotionFluidHandler.fillBottle(stack, toFill);
			stack.shrink(1);
			return fillBottle;
		}

		ItemStack split = stack.copy();
		split.setCount(1);
		// One slot the fluid handler is allowed to swap the item inside: filling a bucket
		// changes which item is held, and an access over a bare stack refuses that.
		ItemStacksResourceHandler carrier = new ItemStacksResourceHandler(1);
		carrier.set(0, ItemResource.of(split), split.getCount());
		ItemAccess access = ItemAccess.forHandlerIndex(carrier, 0);
		ResourceHandler<FluidResource> capability = access.getCapability(Capabilities.Fluid.ITEM);
		if (capability == null)
			return ItemStack.EMPTY;
		try (Transaction transaction = Transaction.openRoot()) {
			int transferred = toFill.isEmpty() ? 0 : capability.insert(FluidResource.of(toFill), toFill.getAmount(), transaction);
			transaction.commit();
		}
		ItemStack container = ItemUtil.getStack(carrier, 0)
			.copy();
		stack.shrink(1);
		return container;
	}

}
