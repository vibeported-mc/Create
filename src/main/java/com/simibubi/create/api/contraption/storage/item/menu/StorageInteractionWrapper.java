package com.simibubi.create.api.contraption.storage.item.menu;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;

public class StorageInteractionWrapper extends ItemHandlerContainer {
	private final Predicate<Player> stillValid;
	private final Consumer<Player> onClose;

	public StorageInteractionWrapper(ResourceHandler<ItemResource> inv, IndexModifier<ItemResource> writable,
		Predicate<Player> stillValid, Consumer<Player> onClose) {
		super(inv, writable);
		this.stillValid = stillValid;
		this.onClose = onClose;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.stillValid.test(player);
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public void stopOpen(ContainerUser containerUser) {
		if (containerUser instanceof Player player)
			this.onClose.accept(player);
	}
}
