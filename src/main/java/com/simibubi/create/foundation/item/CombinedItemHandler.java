package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Several item handlers seen as one, slots and all.
 * <p>
 * NeoForge's own combined handler only offers the transactional half of the API; Create hands these
 * out where slots also get written directly, so the direct write is routed to whichever handler owns
 * the slot.
 */
public class CombinedItemHandler extends CombinedResourceHandler<ItemResource> implements ModifiableItemHandler {

	@SafeVarargs
	public CombinedItemHandler(ModifiableItemHandler... handlers) {
		super(handlers);
	}

	@Override
	public void set(int index, ItemResource resource, int amount) {
		int handlerIndex = getHandlerIndex(index);
		if (getHandlerFromIndex(handlerIndex) instanceof ModifiableItemHandler modifiable)
			modifiable.set(getSlotFromIndex(index, handlerIndex), resource, amount);
	}

}
