package com.simibubi.create.foundation.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.armor.AllArmorMaterials;

import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

@Mixin(ArmorTrim.class)
public abstract class ArmorTrimMixin {
	@Shadow
	@Final
	private Holder<TrimMaterial> material;

	@Shadow
	@Final
	private Holder<TrimPattern> pattern;

	/**
	 * The cardboard variant of a trim texture.
	 * <p>
	 * 26.2 folded the inner and outer texture lookups into one {@code layerAssetId}, and the colour
	 * suffix that used to come from {@code getColorPaletteSuffix} is now the material's asset info for
	 * the equipment being trimmed. The suffix still matters: Create's trim textures go through the
	 * paletted_permutations atlas source, so the sprite is named after the palette.
	 */
	@Unique
	private Identifier create$textureCardboard(boolean leggings, ResourceKey<EquipmentAsset> equipmentAsset) {
		String assetPath = pattern.value()
			.assetId()
			.getPath();
		String colorSuffix = material.value()
			.assets()
			.assetId(equipmentAsset)
			.suffix();
		return Create.asResource("trims/models/armor/card_" + assetPath + (leggings ? "_leggings_" : "_") + colorSuffix);
	}

	@Inject(method = "layerAssetId", at = @At("HEAD"), cancellable = true)
	private void create$swapTexturesForCardboardTrims(String layerAssetPrefix,
		ResourceKey<EquipmentAsset> equipmentAsset, CallbackInfoReturnable<Identifier> cir) {
		if (equipmentAsset != AllArmorMaterials.CARDBOARD_ASSET)
			return;
		cir.setReturnValue(create$textureCardboard(layerAssetPrefix.endsWith("_leggings"), equipmentAsset));
	}
}
