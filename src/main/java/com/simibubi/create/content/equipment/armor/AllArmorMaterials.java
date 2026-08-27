package com.simibubi.create.content.equipment.armor;

import java.util.Map;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;

import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Create's armour materials.
 * <p>
 * Minecraft 26.2 turned {@code ArmorMaterial} from a registry entry into a plain record, and moved
 * the layer textures out of it: a material now names an {@link EquipmentAsset}, and the layers live
 * in {@code assets/create/equipment/<name>.json}. That is also what replaces Create's hand-rolled
 * layered armour rendering - the asset can declare several layers itself.
 */
public class AllArmorMaterials {

	public static final ResourceKey<EquipmentAsset> COPPER_DIVING_ASSET = asset("copper_diving");
	public static final ResourceKey<EquipmentAsset> NETHERITE_DIVING_ASSET = asset("netherite_diving");
	public static final ResourceKey<EquipmentAsset> CARDBOARD_ASSET = asset("cardboard");

	public static final ArmorMaterial COPPER = new ArmorMaterial(7, defense(2, 4, 3, 1, 4), 7,
		AllSoundEvents.COPPER_ARMOR_EQUIP.getMainEventHolder(), 0.0F, 0.0F,
		AllTags.commonItemTag("ingots/copper"), COPPER_DIVING_ASSET);

	public static final ArmorMaterial CARDBOARD = new ArmorMaterial(4, defense(1, 1, 1, 1, 2), 4,
		SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, AllTags.AllItemTags.CARDBOARD_PLATES.tag, CARDBOARD_ASSET);

	/**
	 * Netherite diving gear wears vanilla's netherite stats but Create's own look.
	 */
	public static final ArmorMaterial NETHERITE_DIVING = new ArmorMaterial(37, defense(3, 6, 8, 3, 11), 15,
		SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, AllTags.commonItemTag("ingots/netherite"),
		NETHERITE_DIVING_ASSET);

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Create.asResource(name));
	}

	/**
	 * Defense values in the order Create declared them: boots, leggings, chestplate, helmet, body.
	 */
	private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
		return Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings, ArmorType.CHESTPLATE, chestplate,
			ArmorType.HELMET, helmet, ArmorType.BODY, body);
	}

	/**
	 * 26.2 armour materials are plain values rather than registry entries, so there is nothing left
	 * to register. Kept so the call site in Create's setup does not have to change shape.
	 */
	@Internal
	public static void register(IEventBus eventBus) {
	}
}
