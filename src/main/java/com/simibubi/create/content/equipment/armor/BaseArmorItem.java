package com.simibubi.create.content.equipment.armor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Base for Create's wearable gear.
 * <p>
 * Minecraft 26.2 has no ArmorItem class any more: a piece of armour is a plain {@link Item} whose
 * properties carry the material and slot, and whose look comes from the material's equipment asset
 * rather than from a getArmorTexture override. Create's own subclasses stay for their behaviour -
 * air supply, goggles, and so on.
 */
public class BaseArmorItem extends Item {

	public BaseArmorItem(ArmorMaterial armorMaterial, ArmorType type, Properties properties) {
		super(properties.humanoidArmor(armorMaterial, type)
			.stacksTo(1));
	}
}
