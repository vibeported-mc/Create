package com.simibubi.create.content.equipment.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class BaseArmorItem extends ArmorItem {
	protected final Identifier textureLoc;

	public BaseArmorItem(Holder<ArmorMaterial> armorMaterial, ArmorItem.Type type, Properties properties, Identifier textureLoc) {
		super(armorMaterial, type, properties.stacksTo(1));
		this.textureLoc = textureLoc;
	}

	@Override
	public @Nullable Identifier getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return Identifier.parse(String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", textureLoc.getNamespace(), textureLoc.getPath(), slot == EquipmentSlot.LEGS ? 2 : 1));
	}
}
