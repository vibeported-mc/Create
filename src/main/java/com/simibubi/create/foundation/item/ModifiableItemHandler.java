package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * An item handler whose slots can also be overwritten directly.
 * <p>
 * NeoForge 26.2 split what used to be {@code IItemHandlerModifiable} into two pieces:
 * {@link ResourceHandler} for the transactional transfer API and {@link IndexModifier} for direct
 * mutation. Create needs both together often enough to be worth naming.
 */
public interface ModifiableItemHandler extends ResourceHandler<ItemResource>, IndexModifier<ItemResource> {
}
