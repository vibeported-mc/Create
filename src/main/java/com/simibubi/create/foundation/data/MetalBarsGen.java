package com.simibubi.create.foundation.data;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;

import java.util.function.Supplier;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class MetalBarsGen {
	// TODO 26.2: port datagen to RegistrateBlockModelGenerator. barsBlockState and barsSubModel
	// built the multipart blockstate and its sub-models with NeoForge's removed builder API; the
	// generated assets are committed, so only createBars is needed to register the blocks.

	public static BlockEntry<IronBarsBlock> createBars(String name, boolean specialEdge,
													   Supplier<DataIngredient> ingredient, MapColor color) {
		return Create.registrate().block(name + "_bars", IronBarsBlock::new)
			.initialProperties(() -> Blocks.IRON_BARS)
			.properties(p -> p.sound(SoundType.COPPER)
				.mapColor(color))
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.tag(AllBlockTags.FAN_TRANSPARENT.tag)
			.transform(TagGen.pickaxeOnly())
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .blockstate(barsBlockState(name, specialEdge))
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// .model((c, p) -> {
				// Identifier barsTexture = p.modLoc("block/bars/" + name + "_bars");
				// p.generated(c, barsTexture);
			// })
			
			.recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 4))
			.build()
			.register();
	}

}
