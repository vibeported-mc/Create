package com.simibubi.create.foundation.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * A container seen as a handler whose slots can also be written.
 * <p>
 * This is not a stand-in for anything 26.2 provides. {@link VanillaContainerWrapper} offers only the
 * transactional half of the API, and Create writes container slots outright - a minecart's own
 * inventory on a contraption is written by the dropper actor, and a carried chest is written back on
 * disassembly. 1.21.1 got both from {@code InvWrapper}, which was modifiable. The direct write goes
 * to the container, which is what the wrapper reads from anyway.
 * <p>
 * As with every {@link IndexModifier}, {@link #set} is not part of a transaction and is not undone if
 * one is rolled back - {@code StacksResourceHandler} behaves the same way. Work that has to be
 * reversible goes through insert and extract.
 */
public class ContainerItemHandler extends DelegatingResourceHandler<ItemResource>
	implements IndexModifier<ItemResource> {

	private final Container container;

	public ContainerItemHandler(Container container) {
		super(VanillaContainerWrapper.of(container));
		this.container = container;
	}

	/**
	 * The handler if its slots can already be written, or the block's own container seen that way.
	 * <p>
	 * A block that cannot be written to is never taken as contraption storage, since what it carried
	 * off could not be put back.
	 */
	@Nullable
	public static ResourceHandler<ItemResource> writable(@Nullable ResourceHandler<ItemResource> handler,
		@Nullable BlockEntity be) {

		if (handler instanceof IndexModifier<?>)
			return handler;

		if (be instanceof Container container
			&& (handler == null || handler.size() == container.getContainerSize()))
			return new ContainerItemHandler(container);

		return null;
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		container.setItem(index, resource.toStack(amount));
		container.setChanged();
	}

}
