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

	private static final Map<Item, CustomRenderedItemModelRenderer> ITEMS = new IdentityHashMap<>();

	/**
	 * The renderer is built here rather than when the models are swapped: these renderers hold their
	 * own {@link dev.engine_room.flywheel.lib.model.baked.PartialModel PartialModel}s in static
	 * fields, and a partial only gets baked if it exists before the models are registered.
	 */
	public static void register(Item item, Supplier<CustomRenderedItemModelRenderer> renderer) {
		ITEMS.put(item, renderer.get());
	}

	public static void forEach(BiConsumer<Item, CustomRenderedItemModelRenderer> consumer) {
		ITEMS.forEach(consumer);
	}

}
