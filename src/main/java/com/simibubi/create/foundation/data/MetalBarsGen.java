package com.simibubi.create.foundation.data;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;

import java.util.function.Function;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class MetalBarsGen {

	public static <P extends IronBarsBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> barsBlockState(
		String name, boolean specialEdge) {
		return (c, p) -> {

			MultiVariant post_ends = barsSubModel(p, name, "post_ends", specialEdge);
			MultiVariant post = barsSubModel(p, name, "post", specialEdge);
			MultiVariant cap = barsSubModel(p, name, "cap", specialEdge);
			MultiVariant cap_alt = barsSubModel(p, name, "cap_alt", specialEdge);
			MultiVariant side = barsSubModel(p, name, "side", specialEdge);
			MultiVariant side_alt = barsSubModel(p, name, "side_alt", specialEdge);

			MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get())
				.with(post_ends)

				.with(BlockModelGenerators.condition()
					.term(NORTH, false)
					.term(EAST, false)
					.term(SOUTH, false)
					.term(WEST, false), post)

				.with(BlockModelGenerators.condition()
					.term(NORTH, true)
					.term(EAST, false)
					.term(SOUTH, false)
					.term(WEST, false), cap)

				.with(BlockModelGenerators.condition()
					.term(NORTH, false)
					.term(EAST, true)
					.term(SOUTH, false)
					.term(WEST, false), cap.with(BlockModelGenerators.Y_ROT_90))

				.with(BlockModelGenerators.condition()
					.term(NORTH, false)
					.term(EAST, false)
					.term(SOUTH, true)
					.term(WEST, false), cap_alt)

				.with(BlockModelGenerators.condition()
					.term(NORTH, false)
					.term(EAST, false)
					.term(SOUTH, false)
					.term(WEST, true), cap_alt.with(BlockModelGenerators.Y_ROT_90))

				.with(BlockModelGenerators.condition()
					.term(NORTH, true), side)

				.with(BlockModelGenerators.condition()
					.term(EAST, true), side.with(BlockModelGenerators.Y_ROT_90))

				.with(BlockModelGenerators.condition()
					.term(SOUTH, true), side_alt)

				.with(BlockModelGenerators.condition()
					.term(WEST, true), side_alt.with(BlockModelGenerators.Y_ROT_90));

			p.blockStateOutput.accept(builder);
		};
	}

	private static MultiVariant barsSubModel(RegistrateBlockModelGenerator p, String name, String suffix,
		boolean specialEdge) {
		Material barsTexture = new Material(p.modLoc("block/bars/" + name + "_bars"));
		Material edgeTexture =
			specialEdge ? new Material(p.modLoc("block/bars/" + name + "_bars_edge")) : barsTexture;
		return BlockModelGenerators.plainVariant(p.getBuilder()
			.parent(p.modLoc("block/bars/" + suffix))
			.texture(TextureSlot.BARS, barsTexture)
			.texture(TextureSlot.PARTICLE, barsTexture)
			.texture(TextureSlot.EDGE, edgeTexture)
			.build(p.modLoc("block/" + name + "_" + suffix)));
	}

	public static BlockEntry<IronBarsBlock> createBars(String name, boolean specialEdge,
													   Function<RegistrateRecipeProvider, DataIngredient> ingredient, MapColor color) {
		return Create.registrate().block(name + "_bars", IronBarsBlock::new)
			.initialProperties(() -> Blocks.IRON_BARS)
			.properties(p -> p.sound(SoundType.COPPER)
				.mapColor(color))
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.tag(AllBlockTags.FAN_TRANSPARENT.tag)
			.transform(TagGen.pickaxeOnly())
			.blockstate(() -> barsBlockState(name, specialEdge))
			.item()
			.model(() -> (c, p) -> p.generateFlatItem(c.getEntry(),
				new Material(p.modLoc("block/bars/" + name + "_bars"))))
			.recipe((c, p) -> p.stonecutting(ingredient.apply(p), RecipeCategory.DECORATIONS, c::get, 4))
			.build()
			.register();
	}

}
