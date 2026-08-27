package com.simibubi.create.content.equipment.tool;

import com.simibubi.create.AllTags;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * Create's tool materials.
 * <p>
 * Minecraft 26.2 replaced the {@code Tier} interface with the {@link ToolMaterial} record, and the
 * repair ingredient is a tag rather than an {@code Ingredient} supplier.
 */
public class AllToolMaterials {

	public static final ToolMaterial CARDBOARD = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 0, 1, 2, 1,
		AllTags.AllItemTags.CARDBOARD_PLATES.tag);

	private AllToolMaterials() {
	}
}
