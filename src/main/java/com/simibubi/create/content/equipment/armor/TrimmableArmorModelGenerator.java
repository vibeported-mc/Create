package com.simibubi.create.content.equipment.armor;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import com.simibubi.create.Create;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Item models for armour that can carry a trim.
 * <p>
 * 26.2 generates these itself. A trimmed model used to be an override on the base model, picked by a
 * predicate on {@code trim_type}, and building one meant reaching into the model builder's texture
 * map by reflection to add the trim layer. Now a trimmable item is one call: the layers come from
 * the material's {@link net.minecraft.world.item.equipment.EquipmentAsset}, and the slot decides
 * which trim sprites are used.
 */
public class TrimmableArmorModelGenerator {

	public static <T extends BaseArmorItem> void generate(DataGenContext<Item, T> c, RegistrateItemModelGenerator p) {
		T item = c.get();
		p.generateTrimmableItem(item, item.getMaterial()
			.assetId(), slotTrimPrefix(item.getArmorType()), false);
	}

	/**
	 * Create draws its cardboard trims from its own sprites rather than the vanilla ones, so the prefix
	 * the trim layer is built from points into Create's namespace.
	 */
	private static Identifier slotTrimPrefix(ArmorType type) {
		return Create.asResource("trims/items/card_" + switch (type) {
			case HELMET -> "helmet";
			case CHESTPLATE, BODY -> "chestplate";
			case LEGGINGS -> "leggings";
			case BOOTS -> "boots";
		} + "_trim");
	}

}
