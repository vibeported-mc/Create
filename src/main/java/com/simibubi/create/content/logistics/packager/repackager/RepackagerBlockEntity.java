package com.simibubi.create.content.logistics.packager.repackager;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.content.logistics.packager.PackagingRequest;

import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
public class RepackagerBlockEntity extends PackagerBlockEntity {

	public PackageRepackageHelper repackageHelper;

	public RepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		repackageHelper = new PackageRepackageHelper();
	}

	public boolean unwrapBox(ItemStack box, boolean simulate) {
		if (animationTicks > 0)
			return false;

		ResourceHandler<ItemResource> targetInv = targetInventory.getInventory();
		if (targetInv == null || targetInv instanceof PackagerItemHandler)
			return false;

		boolean targetIsCreativeCrate = targetInv instanceof BottomlessItemHandler;
		boolean anySpace = false;

		for (int slot = 0; slot < targetInv.size(); slot++) {
			ItemStack remainder;
			try (Transaction transaction = Transaction.openRoot()) {
				int transferred = box.isEmpty() ? 0 : targetInv.insert(slot, ItemResource.of(box), box.getCount(), transaction);
				remainder = transferred == box.getCount() ? ItemStack.EMPTY : box.copyWithCount(box.getCount() - transferred);
				if (!simulate)
					transaction.commit();
			}
			if (!remainder.isEmpty())
				continue;
			anySpace = true;
			break;
		}

		if (!targetIsCreativeCrate && !anySpace)
			return false;
		if (simulate)
			return true;

		computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
		previouslyUnwrapped = box;
		animationInward = true;
		animationTicks = CYCLE;
		notifyUpdate();
		return true;
	}

	@Override
	public void recheckIfLinksPresent() {
	}

	@Override
	public boolean redstoneModeActive() {
		return true;
	}

	public void attemptToSend(List<PackagingRequest> queuedRequests) {
		if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0)
			return;
		if (!queuedExitingPackages.isEmpty())
			return;

		ResourceHandler<ItemResource> targetInv = targetInventory.getInventory();
		if (targetInv == null || targetInv instanceof PackagerItemHandler)
			return;

		attemptToRepackage(targetInv);
		if (heldBox.isEmpty())
			return;

		updateSignAddress();
		if (!signBasedAddress.isBlank())
			PackageItem.addAddress(heldBox, signBasedAddress);
	}

	protected void attemptToRepackage(ResourceHandler<ItemResource> targetInv) {
		repackageHelper.clear();
		int completedOrderId = -1;

		for (int slot = 0; slot < targetInv.size(); slot++) {
			ItemStack extracted;
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred2Resource = targetInv.getResource(slot);
				int transferred2 = transferred2Resource.isEmpty() ? 0 : targetInv.extract(slot, transferred2Resource, 1, transaction);
				extracted = transferred2 <= 0 ? ItemStack.EMPTY : transferred2Resource.toStack(transferred2);
			}
			if (extracted.isEmpty() || !PackageItem.isPackage(extracted))
				continue;

			if (!repackageHelper.isFragmented(extracted)) {
				try (Transaction transaction = Transaction.openRoot()) {
					ItemResource transferredResource = targetInv.getResource(slot);
					int transferred = transferredResource.isEmpty() ? 0 : targetInv.extract(slot, transferredResource, 1, transaction);
					transaction.commit();
				}
				heldBox = extracted.copy();
				animationInward = false;
				animationTicks = CYCLE;
				notifyUpdate();
				return;
			}

			completedOrderId = repackageHelper.addPackageFragment(extracted);
			if (completedOrderId != -1)
				break;
		}

		if (completedOrderId == -1)
			return;

		List<BigItemStack> boxesToExport = repackageHelper.repack(completedOrderId, level.getRandom());

		for (int slot = 0; slot < targetInv.size(); slot++) {
			ItemStack extracted;
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred3Resource = targetInv.getResource(slot);
				int transferred3 = transferred3Resource.isEmpty() ? 0 : targetInv.extract(slot, transferred3Resource, 1, transaction);
				extracted = transferred3 <= 0 ? ItemStack.EMPTY : transferred3Resource.toStack(transferred3);
			}
			if (extracted.isEmpty() || !PackageItem.isPackage(extracted))
				continue;
			if (PackageItem.getOrderId(extracted) != completedOrderId)
				continue;
			try (Transaction transaction = Transaction.openRoot()) {
				ItemResource transferred2Resource = targetInv.getResource(slot);
				int transferred2 = transferred2Resource.isEmpty() ? 0 : targetInv.extract(slot, transferred2Resource, 1, transaction);
				transaction.commit();
			}
		}

		if (boxesToExport.isEmpty())
			return;

		if (computerBehaviour.hasAttachedComputer()) {
			for (BigItemStack box : boxesToExport) {
				computerBehaviour.prepareComputerEvent(new RepackageEvent(box.stack, box.count));
			}
		}
		queuedExitingPackages.addAll(boxesToExport);
		notifyUpdate();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.Item.BLOCK,
			AllBlockEntityTypes.REPACKAGER.get(),
			(be, context) -> be.inventory
		);

		if (Mods.COMPUTERCRAFT.isLoaded()) {
			event.registerBlockEntity(
				PeripheralCapability.get(),
				AllBlockEntityTypes.REPACKAGER.get(),
				(be, context) -> be.computerBehaviour.getPeripheralCapability()
			);
		}
	}

}
