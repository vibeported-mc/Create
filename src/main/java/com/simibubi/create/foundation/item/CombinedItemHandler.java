package com.simibubi.create.foundation.item;

import java.util.SequencedCollection;

import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Several item handlers seen as one, whose slots can also be written.
 * <p>
 * This is not a stand-in for anything 26.2 provides. {@link CombinedResourceHandler} covers only the
 * transactional half of the API, and the three methods that say which handler owns a slot are
 * protected, so routing a direct write to the right one can only be done from a subclass. Create
 * hands combined inventories to menus, to contraption disassembly and to the millstone and basin,
 * all of which write slots outright, so the routing has to live somewhere.
 * <p>
 * A write lands on whichever handler owns that slot, and is dropped if that handler cannot take one -
 * the same thing the combined handler does when a slot refuses a transfer.
 */
public class CombinedItemHandler extends CombinedResourceHandler<ItemResource>
	implements IndexModifier<ItemResource> {

	@SafeVarargs
	public CombinedItemHandler(ResourceHandler<ItemResource>... handlers) {
		super(handlers);
	}

	public CombinedItemHandler(SequencedCollection<? extends ResourceHandler<ItemResource>> handlers) {
		super(handlers);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		int handlerIndex = getHandlerIndex(index);

		if (getHandlerFromIndex(handlerIndex) instanceof IndexModifier<?> modifier) {
			@SuppressWarnings("unchecked")
			IndexModifier<ItemResource> writable = (IndexModifier<ItemResource>) modifier;
			writable.set(getSlotFromIndex(index, handlerIndex), resource, amount);
		}
	}

}
