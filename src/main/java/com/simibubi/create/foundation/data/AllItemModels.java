package com.simibubi.create.foundation.data;

import com.simibubi.create.content.legacy.ChromaticCompoundColor;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The item models Create writes by hand, kept away from {@code AllItems}.
 * <p>
 * Every method here names a client datagen type, and the JVM resolves those when it verifies the
 * class that holds them -- so writing these as lambdas inside {@code AllItems} stopped that class
 * from loading on a dedicated server, and with it every item Create registers. Called through a
 * static from a model supplier, they are resolved only when datagen actually runs.
 */
public class AllItemModels {

	/** A tint source per layer, named by the model, rather than a colour handler bound to the item. */
	public static void chromaticCompound(DataGenContext<Item, ?> c, RegistrateItemModelGenerator p) {
		p.itemModelOutput.accept(c.getEntry(),
			ItemModelUtils.tintedModel(p.modLoc("item/" + c.getName()), new ChromaticCompoundColor(0),
				new ChromaticCompoundColor(1), new ChromaticCompoundColor(2)));
	}

	/**
	 * Worn on the head the goggles are a block model rather than the flat item sprite. A model cannot
	 * choose per display context in code any more, so this is a select on display_context.
	 */
	public static void goggles(DataGenContext<Item, ?> c, RegistrateItemModelGenerator p) {
		Identifier flat = ModelTemplates.FLAT_ITEM.create(c.getEntry(),
			TextureMapping.layer0(p.modItemTexture(c.getName())), p.modelOutput);
		p.itemModelOutput.accept(c.getEntry(), ItemModelUtils.select(new DisplayContext(),
			ItemModelUtils.plainModel(flat),
			ItemModelUtils.when(ItemDisplayContext.HEAD,
				ItemModelUtils.plainModel(p.modLoc("block/goggles")))));
	}

	private AllItemModels() {}
}
