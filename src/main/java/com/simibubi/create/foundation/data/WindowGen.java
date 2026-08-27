package com.simibubi.create.foundation.data;

import net.minecraft.core.registries.BuiltInRegistries;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.GlassPaneCTBehaviour;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.common.Tags;

public class WindowGen {
	private static final CreateRegistrate REGISTRATE = Create.registrate();

	private static Properties glassProperties(Properties p) {
		return p.isValidSpawn(WindowGen::never)
			.isRedstoneConductor(WindowGen::never)
			.isSuffocating(WindowGen::never)
			.isViewBlocking(WindowGen::never);
	}

	private static boolean never(BlockState p_235436_0_, BlockGetter p_235436_1_, BlockPos p_235436_2_) {
		return false;
	}

	private static Boolean never(BlockState p_235427_0_, BlockGetter p_235427_1_, BlockPos p_235427_2_,
								 EntityType<?> p_235427_3_) {
		return false;
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock) {
		return woodenWindowBlock(woodType, planksBlock, false);
	}

	public static BlockBuilder<WindowBlock, CreateRegistrate> randomisedWindowBlock(String name,
																					Supplier<? extends ItemLike> ingredient, boolean translucent,
																					Supplier<MapColor> color) {
		Identifier end_texture = Create.asResource(palettesDir() + name + "_end");
		Identifier side_texture = Create.asResource(palettesDir() + name);
		Function<Integer, Identifier> ends = i -> Create.asResource(palettesDir() + name + "_" + i + "_end");
		return windowBlock(name, ingredient, null, translucent, n -> end_texture, n -> side_texture, color)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .blockstate((c, p) -> p.simpleBlock(c.get(), ConfiguredModel.builder()
										// .modelFile(p.models()
											// .cubeColumn(c.getName() + "_1", side_texture, ends.apply(1)))
										// .nextModel()
										// .modelFile(p.models()
											// .cubeColumn(c.getName() + "_2", side_texture, ends.apply(2)))
										// .nextModel()
										// .modelFile(p.models()
											// .cubeColumn(c.getName() + "_3", side_texture, ends.apply(3)))
										// .nextModel()
										// .modelFile(p.models()
											// .cubeColumn(c.getName() + "_4", side_texture, ends.apply(4)))
										// .build()))
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// .model((c, p) -> p.cubeColumn(c.getName(), side_texture, ends.apply(1)))
			
			.build();
	}

	public static BlockEntry<WindowBlock> customWindowBlock(String name, Supplier<? extends ItemLike> ingredient,
															Supplier<CTSpriteShiftEntry> ct, boolean translucent,
															Supplier<MapColor> color) {
		NonNullFunction<String, Identifier> end_texture = n -> Create.asResource(palettesDir() + name + "_end");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, ingredient, ct, translucent, end_texture, side_texture, color).register();
	}

	public static BlockEntry<WindowBlock> woodenWindowBlock(WoodType woodType, Block planksBlock,
															boolean translucent) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		NonNullFunction<String, Identifier> end_texture =
			$ -> Identifier.withDefaultNamespace("block/" + woodName + "_planks");
		NonNullFunction<String, Identifier> side_texture = n -> Create.asResource(palettesDir() + n);
		return windowBlock(name, () -> planksBlock, () -> AllSpriteShifts.getWoodenWindow(woodType),
			translucent, end_texture, side_texture, planksBlock::defaultMapColor).register();
	}

	public static BlockBuilder<WindowBlock, CreateRegistrate> windowBlock(String name,
																		  Supplier<? extends ItemLike> ingredient, Supplier<CTSpriteShiftEntry> ct,
																		  boolean translucent,
																		  NonNullFunction<String, Identifier> endTexture, NonNullFunction<String, Identifier> sideTexture,
																		  Supplier<MapColor> color) {
		return REGISTRATE.block(name, p -> new WindowBlock(p, translucent))
			.onRegister(ct == null ? $ -> {
			} : connectedTextures(() -> new HorizontalCTBehaviour(ct.get())))
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, c.get(), 2)
				// .pattern(" # ")
				// .pattern("#X#")
				// .define('#', ingredient.get())
					// .define('X', DataIngredient.tag(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.GLASS_BLOCKS_COLORLESS)).toVanilla())
				// .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(ingredient.get()))
				// .save(p))
			
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			.properties(p -> p.mapColor(color.get()))
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((t, g) -> t.dropWhenSilkTouch(g))
			
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
										// .cubeColumn(c.getName(), sideTexture.apply(c.getName()), endTexture.apply(c.getName()))))
			
			
			.tag(BlockTags.IMPERMEABLE)
			.simpleItem();
	}

	public static BlockEntry<ConnectedGlassBlock> framedGlass(String name,
															  Supplier<ConnectedTextureBehaviour> behaviour) {
		return REGISTRATE.block(name, ConnectedGlassBlock::new)
			.onRegister(connectedTextures(behaviour))
			.initialProperties(() -> Blocks.GLASS)
			.properties(WindowGen::glassProperties)
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((t, g) -> t.dropWhenSilkTouch(g))
			
				// TODO 26.2: port datagen to the new recipe/loot builders
				// .recipe((c, p) -> p.stonecutting(DataIngredient.tag(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.GLASS_BLOCKS_COLORLESS)),
				// RecipeCategory.BUILDING_BLOCKS, c::get))
				
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .blockstate((c, p) -> BlockStateGen.cubeAll(c, p, "palettes/", "framed_glass"))
			
			
				.tag(Tags.Blocks.GLASS_BLOCKS_COLORLESS, BlockTags.IMPERMEABLE)
			.item()
				.tag(Tags.Items.GLASS_BLOCKS_COLORLESS)
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// .model((c, p) -> p.cubeColumn(c.getName(), p.modLoc(palettesDir() + c.getName()),
				// p.modLoc("block/palettes/framed_glass")))
			
			.build()
			.register();
	}

	public static BlockEntry<ConnectedGlassPaneBlock> framedGlassPane(String name, Supplier<? extends Block> parent,
																	  Supplier<CTSpriteShiftEntry> ctshift) {
		Identifier sideTexture = Create.asResource(palettesDir() + "framed_glass");
		Identifier itemSideTexture = Create.asResource(palettesDir() + name);
		Identifier topTexture = Create.asResource(palettesDir() + "framed_glass_pane_top");
		return connectedGlassPane(name, parent, ctshift, sideTexture, itemSideTexture, topTexture, true)
			.register();
	}

	public static BlockBuilder<ConnectedGlassPaneBlock, CreateRegistrate> customWindowPane(String name,
																						   Supplier<? extends Block> parent, Supplier<CTSpriteShiftEntry> ctshift) {
		Identifier topTexture = Create.asResource(palettesDir() + name + "_pane_top");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, ctshift, sideTexture, sideTexture, topTexture, false);
	}

	public static BlockEntry<ConnectedGlassPaneBlock> woodenWindowPane(WoodType woodType,
																	   Supplier<? extends Block> parent) {
		String woodName = woodType.name();
		String name = woodName + "_window";
		Identifier topTexture = Identifier.withDefaultNamespace("block/" + woodName + "_planks");
		Identifier sideTexture = Create.asResource(palettesDir() + name);
		return connectedGlassPane(name, parent, () -> AllSpriteShifts.getWoodenWindow(woodType), sideTexture,
			sideTexture, topTexture, false).register();
	}

	public static BlockEntry<GlassPaneBlock> standardGlassPane(String name, Supplier<? extends Block> parent,
															   Identifier sideTexture, Identifier topTexture) {
		return glassPane(name, parent, sideTexture, topTexture, GlassPaneBlock::new, $ -> {
		}, true).register();
	}

	private static BlockBuilder<ConnectedGlassPaneBlock, CreateRegistrate> connectedGlassPane(String name,
																							  Supplier<? extends Block> parent, Supplier<CTSpriteShiftEntry> ctshift, Identifier sideTexture,
																							  Identifier itemSideTexture, Identifier topTexture, boolean colorless) {
		NonNullConsumer<? super ConnectedGlassPaneBlock> connectedTextures = ctshift == null ? $ -> {
		} : connectedTextures(() -> new GlassPaneCTBehaviour(ctshift.get()));
		return glassPane(name, parent, itemSideTexture, topTexture, ConnectedGlassPaneBlock::new,
			connectedTextures, colorless);
	}

	private static <G extends GlassPaneBlock> BlockBuilder<G, CreateRegistrate> glassPane(String name,
																						  Supplier<? extends Block> parent, Identifier sideTexture, Identifier topTexture,
																						  NonNullFunction<Properties, G> factory,
																						  NonNullConsumer<? super G> connectedTextures, boolean colorless) {
		name += "_pane";


		ItemBuilder<BlockItem, BlockBuilder<G, CreateRegistrate>> itemBuilder = REGISTRATE.block(name, factory)
			.onRegister(connectedTextures)
			.initialProperties(() -> Blocks.GLASS_PANE)
			.properties(p -> p.mapColor(parent.get()
				.defaultMapColor()))
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .blockstate(stateProvider)
			
			
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> {
				// ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, c.get(), 16)
					// .pattern("###")
					// .pattern("###")
					// .define('#', parent.get())
					// .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(parent.get()))
					// .save(p);
				// if (colorless)
					// p.stonecutting(DataIngredient.tag(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.GLASS_PANES_COLORLESS)), RecipeCategory.BUILDING_BLOCKS,
						// c::get);
			// })
			
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((t, g) -> t.dropWhenSilkTouch(g))
			
			.item();

		if (colorless)
			itemBuilder.tag(Tags.Items.GLASS_PANES, Tags.Items.GLASS_PANES_COLORLESS);
		else
			itemBuilder.tag(Tags.Items.GLASS_PANES);

		BlockBuilder<G, CreateRegistrate> blockBuilder = itemBuilder
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// .model((c, p) -> p.generated(c, sideTexture))
			
			.build();

		if (colorless)
			blockBuilder.tag(Tags.Blocks.GLASS_PANES, Tags.Blocks.GLASS_PANES_COLORLESS);
		else
			blockBuilder.tag(Tags.Blocks.GLASS_PANES);

		return blockBuilder;
	}

	private static String palettesDir() {
		return "block/palettes/";
	}

}
