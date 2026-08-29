package com.simibubi.create.content.equipment.armor;

import java.util.function.BiConsumer;

import com.simibubi.create.Create;

import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;

/**
 * The layers each of Create's armour materials draws.
 * <p>
 * 26.2 reads these out of assets/create/equipment/&lt;name&gt;.json instead of the armour item
 * describing itself in code, which is what Create's old LayeredArmorItem was doing.
 * <p>
 * Only the layers Create actually has textures for are declared: the vanilla helper would also add a
 * humanoid_baby layer, and Create ships no sprites under that folder.
 */
public class AllEquipmentAssets extends EquipmentAssetProvider {

	public AllEquipmentAssets(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
		output.accept(AllArmorMaterials.CARDBOARD_ASSET, humanoid("cardboard", true));
		output.accept(AllArmorMaterials.COPPER_DIVING_ASSET, humanoid("copper_diving", false));
		output.accept(AllArmorMaterials.NETHERITE_DIVING_ASSET, humanoid("netherite_diving", true));
	}

	private static EquipmentClientInfo humanoid(String texture, boolean withLeggings) {
		Identifier id = Create.asResource(texture);
		EquipmentClientInfo.Builder builder = EquipmentClientInfo.builder()
			.addLayers(EquipmentClientInfo.LayerType.HUMANOID, new EquipmentClientInfo.Layer(id));
		if (withLeggings)
			builder.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, new EquipmentClientInfo.Layer(id));
		return builder.build();
	}

	@Override
	public String getName() {
		return "Create's Equipment Assets";
	}

}
