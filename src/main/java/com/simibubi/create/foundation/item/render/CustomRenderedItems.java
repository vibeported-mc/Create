package com.simibubi.create.foundation.item.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The items Create draws itself.
 * <p>
 * These used to declare their renderer through NeoForge's client item extensions, which 26.2
 * dropped along with the block-entity-without-level renderer. They are listed here instead, and
 * {@link com.simibubi.create.foundation.model.ModelSwapper} wraps each one's baked model so the
 * renderer is reached through the item's render state.
 */
@OnlyIn(Dist.CLIENT)
public class CustomRenderedItems {

	private static final Map<Item, Supplier<CustomRenderedItemModelRenderer>> ITEMS = new IdentityHashMap<>();

	public static void register(Item item, Supplier<CustomRenderedItemModelRenderer> renderer) {
		ITEMS.put(item, renderer);
	}

	public static void forEach(BiConsumer<Item, CustomRenderedItemModelRenderer> consumer) {
		ITEMS.forEach((item, renderer) -> consumer.accept(item, renderer.get()));
	}

}
