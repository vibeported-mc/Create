package com.simibubi.create.api.contraption.storage.item.simple;

import net.neoforged.neoforge.transfer.IndexModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ContainerItemHandler;

import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.foundation.codec.CreateCodecs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
/**
 * Widely-applicable mounted storage implementation.
 * Gets an item handler from the mounted block, copies it to an ItemStacksResourceHandler,
 * and then copies the inventory back to the target when unmounting.
 * All blocks for which this mounted storage is registered must provide an
 * {@link ResourceHandler<ItemResource>} to {@link Capabilities.Item#BLOCK}.
 * <br>
 * To use this implementation, either register {@link AllMountedStorageTypes#SIMPLE} to your block
 * manually, or add your block to the {@link AllTags.AllBlockTags#SIMPLE_MOUNTED_STORAGE} tag.
 * It is also possible to extend this class to create your own implementation.
 */
public class SimpleMountedStorage extends WrapperMountedItemStorage<ItemStacksResourceHandler> {
	public static final MapCodec<SimpleMountedStorage> CODEC = codec(SimpleMountedStorage::new);

	public SimpleMountedStorage(MountedItemStorageType<?> type, ResourceHandler<ItemResource> handler) {
		super(type, copyToItemStackHandler(handler));
	}

	public SimpleMountedStorage(ResourceHandler<ItemResource> handler) {
		this(AllMountedStorageTypes.SIMPLE.get(), handler);
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (be == null)
			return;

		ResourceHandler<ItemResource> cap =
			ContainerItemHandler.writable(level.getCapability(Capabilities.Item.BLOCK, pos, null), be);
		if (cap != null) {
			validate(cap).ifPresent(handler -> {
				for (int i = 0; i < this.size(); i++) {
					ItemStack stack = ItemUtil.getStack(this, i);
					handler.set(i, ItemResource.of(stack), stack.getCount());
				}
			});
		}
	}

	/**
	 * Make sure the targeted handler is valid for copying items back into.
	 * It is highly recommended to call super in overrides.
	 * <p>
	 * Writing slots outright lives on {@link IndexModifier} in 26.2, so a handler that only offers
	 * the transactional half cannot be written back into and is refused here.
	 */
	protected Optional<IndexModifier<ItemResource>> validate(ResourceHandler<ItemResource> handler) {
		if (handler.size() == this.size() && handler instanceof IndexModifier<?> modifiable) {
			@SuppressWarnings("unchecked")
			IndexModifier<ItemResource> writable = (IndexModifier<ItemResource>) modifiable;
			return Optional.of(writable);
		} else {
			return Optional.empty();
		}
	}

	public static <T extends SimpleMountedStorage> MapCodec<T> codec(Function<ResourceHandler<ItemResource>, T> factory) {
		return CreateCodecs.ITEM_STACK_HANDLER.xmap(factory, storage -> storage.wrapped).fieldOf("value");
	}
}
