package com.simibubi.create.content.logistics.packager;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.packager.InventoryIdentifier;

/**
 * An item inventory, possibly with an associated InventoryIdentifier.
 */
public record IdentifiedInventory(@Nullable InventoryIdentifier identifier, ResourceHandler<ItemResource> handler) {
}
