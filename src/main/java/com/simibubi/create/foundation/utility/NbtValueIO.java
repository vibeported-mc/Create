package com.simibubi.create.foundation.utility;

import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Bridges Minecraft 26.2's ValueIO to the {@link CompoundTag} shape Create writes.
 * <p>
 * Entities and block entities save through {@link ValueOutput} and {@link ValueInput} now. Create's
 * serialisation is hand-written against CompoundTag throughout - often through helpers shared with
 * packets and contraption storage, which still speak tags - so these move a whole tag across the
 * boundary in one piece rather than rewriting every body. The stored shape is identical either way,
 * since ValueIO writes the same keys at the same level.
 */
public class NbtValueIO {

	@SuppressWarnings("deprecation")
	private static final MapCodec<CompoundTag> WHOLE = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

	/**
	 * Write a tag's entries at the root of the output.
	 */
	public static void store(ValueOutput output, CompoundTag tag) {
		output.store(tag);
	}

	/**
	 * Read everything at the root of the input back as a tag.
	 */
	public static CompoundTag read(ValueInput input) {
		return input.read(WHOLE)
			.orElseGet(CompoundTag::new);
	}

	/**
	 * A fresh tag holding whatever the writer stores.
	 * <p>
	 * For the small, registry-free pieces Create keeps as nested tags - animation state and the like -
	 * which now write themselves through {@link ValueOutput}.
	 */
	public static CompoundTag toTag(Consumer<ValueOutput> writer) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, RegistryAccess.EMPTY);
		writer.accept(output);
		return output.buildResult();
	}

	/**
	 * A view of a tag that can be handed to something reading through {@link ValueInput}.
	 */
	public static ValueInput fromTag(CompoundTag tag) {
		return fromTag(tag, RegistryAccess.EMPTY);
	}

	/**
	 * A view of a tag that can resolve registry entries while it is read.
	 */
	public static ValueInput fromTag(CompoundTag tag, HolderLookup.Provider registries) {
		return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
	}

	/**
	 * A tag holding what the given object serialises, for the parts of Create that still hand tags
	 * around.
	 */
	public static CompoundTag serialize(ValueIOSerializable target, HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		target.serialize(output);
		return output.buildResult();
	}

	/**
	 * Read an object's state back out of a tag.
	 */
	public static void deserialize(ValueIOSerializable target, CompoundTag tag, HolderLookup.Provider registries) {
		target.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));

		// GAMETEST FIX - a stopgap: what is written this way wants a datafixer, not a read-time fallback.
		// An inventory written before the handler serialized through ValueIO kept its slots in an "Items"
		// list, each naming its own "Slot"; read the new way that is not found and the whole inventory
		// comes back empty, without complaint. Those are still about - in worlds from an older version, in
		// schematics, in the structures the game tests are built from.
		if (tag.contains(ITEMS) && target instanceof ResourceHandler<?> handlingResources)
			readLegacyItems(tag, handlingResources);
	}

	@SuppressWarnings("unchecked")
	private static void readLegacyItems(CompoundTag tag, ResourceHandler<?> handlingResources) {
		ResourceHandler<ItemResource> handler = (ResourceHandler<ItemResource>) handlingResources;

		for (int slot = 0; slot < handler.size(); slot++)
			if (!ItemUtil.getStack(handler, slot)
				.isEmpty())
				return; // already read, whatever is in the list is the same thing said twice

		for (Tag entry : tag.getListOrEmpty(ITEMS)) {
			if (!(entry instanceof CompoundTag stackTag))
				continue;

			int slot = stackTag.getIntOr("Slot", -1);
			ItemStack stack = legacyStack(stackTag);

			if (slot >= 0 && slot < handler.size() && !stack.isEmpty())
				writeSlot(handler, slot, stack);
		}
	}

	/**
	 * Writing a slot directly, whichever shape the handler offers it in.
	 * <p>
	 * 26.2 puts direct writes on {@link IndexModifier}, which {@code StacksResourceHandler} provides
	 * as a plain method without declaring the interface - so both are accepted rather than the caller
	 * having to know which it is holding.
	 */
	@SuppressWarnings("unchecked")
	private static void writeSlot(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
		ItemResource resource = ItemResource.of(stack);

		if (handler instanceof IndexModifier<?> modifier)
			((IndexModifier<ItemResource>) modifier).set(slot, resource, stack.getCount());
		else if (handler instanceof StacksResourceHandler<?, ?> stacks)
			((StacksResourceHandler<?, ItemResource>) stacks).set(slot, resource, stack.getCount());
	}

	/**
	 * A stack from before an item carried components: a name and a count, and nothing else that survives.
	 */
	private static ItemStack legacyStack(CompoundTag stackTag) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(stackTag.getStringOr("id", "minecraft:air")))
			.map(item -> new ItemStack(item, stackTag.getByteOr("Count", (byte) 1)))
			.orElse(ItemStack.EMPTY);
	}

	private static final String ITEMS = "Items";

	/**
	 * A player's inventory as the plain list of stacks Create stores under a key of its own.
	 * <p>
	 * 26.2 saves an inventory into a typed ValueIO list rather than a {@link ListTag}; the entries are
	 * the same either way.
	 */
	public static ListTag saveInventory(Inventory inventory, HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		inventory.save(output.list(ITEMS, ItemStackWithSlot.CODEC));
		return output.buildResult()
			.getList(ITEMS)
			.orElseGet(ListTag::new);
	}

	/**
	 * Fill a player's inventory from a list written by {@link #saveInventory}.
	 */
	public static void loadInventory(Inventory inventory, ListTag list, HolderLookup.Provider registries) {
		for (Tag entry : list)
			if (entry instanceof CompoundTag stackTag)
				unwrapLegacyEnchantments(stackTag);

		CompoundTag tag = new CompoundTag();
		tag.put(ITEMS, list);
		inventory.load(fromTag(tag, registries).listOrEmpty(ITEMS, ItemStackWithSlot.CODEC));
	}

	/**
	 * GAMETEST FIX - a stopgap: what is written this way wants a datafixer, not a read-time fallback.
	 * <p>
	 * An item written before 26.2 keeps what it is enchanted with one step further in, under "levels".
	 * Read now, that shape is not recognised, and the item comes back unenchanted rather than unread -
	 * a deployer holding a pickaxe of efficiency five swings it like a plain one.
	 */
	private static void unwrapLegacyEnchantments(CompoundTag stackTag) {
		CompoundTag components = stackTag.getCompoundOrEmpty("components");
		CompoundTag enchantments = components.getCompoundOrEmpty("minecraft:enchantments");

		if (enchantments.contains("levels"))
			components.put("minecraft:enchantments", enchantments.getCompoundOrEmpty("levels"));
	}

	private NbtValueIO() {
	}

}
