package com.simibubi.create.content.decoration.palettes;

import static com.simibubi.create.content.decoration.palettes.PaletteBlockPartial.ALL_PARTIALS;
import static com.simibubi.create.content.decoration.palettes.PaletteBlockPartial.FOR_POLISHED;
import static com.simibubi.create.content.decoration.palettes.PaletteBlockPattern.PatternNameType.PREFIX;
import static com.simibubi.create.content.decoration.palettes.PaletteBlockPattern.PatternNameType.SUFFIX;
import static com.simibubi.create.content.decoration.palettes.PaletteBlockPattern.PatternNameType.WRAP;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.block.connected.RotatedPillarCTBehaviour;
import com.tterrag.registrate.providers.DataGenContext;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PaletteBlockPattern {

	public static final PaletteBlockPattern

	CUT =
		create("cut", PREFIX, ALL_PARTIALS),

		BRICKS = create("cut_bricks", WRAP, ALL_PARTIALS).textures("brick"),

		SMALL_BRICKS = create("small_bricks", WRAP, ALL_PARTIALS).textures("small_brick"),

		POLISHED = create("polished_cut", PREFIX, FOR_POLISHED).textures("polished", "slab"),

		LAYERED = create("layered", PREFIX).blockStateFactory(p -> p::cubeColumn)
			.textures("layered", "cap")
			.connectedTextures(v -> new HorizontalCTBehaviour(ct(v, CTs.LAYERED), ct(v, CTs.CAP))),

		PILLAR = create("pillar", SUFFIX).blockStateFactory(p -> p::pillar)
			.block(ConnectedPillarBlock::new)
			.textures("pillar", "cap")
			.connectedTextures(v -> new RotatedPillarCTBehaviour(ct(v, CTs.PILLAR), ct(v, CTs.CAP)))

	;

	public static final PaletteBlockPattern[] VANILLA_RANGE = { CUT, POLISHED, BRICKS, SMALL_BRICKS, LAYERED, PILLAR };

	public static final PaletteBlockPattern[] STANDARD_RANGE = { CUT, POLISHED, BRICKS, SMALL_BRICKS, LAYERED, PILLAR };

	static final String TEXTURE_LOCATION = "block/palettes/stone_types/%s/%s";

	private PatternNameType nameType;
	private String[] textures;
	private String id;
	private boolean isTranslucent;
	private TagKey<Block>[] blockTags;
	private TagKey<Item>[] itemTags;
	private Optional<Function<String, ConnectedTextureBehaviour>> ctFactory;

	private IPatternBlockStateGenerator blockStateGenerator;
	private NonNullFunction<Properties, ? extends Block> blockFactory;
	private NonNullFunction<NonNullSupplier<Block>, NonNullBiConsumer<DataGenContext<Block, ? extends Block>, RegistrateRecipeProvider>> additionalRecipes;
	private PaletteBlockPartial<? extends Block>[] partials;

	@OnlyIn(Dist.CLIENT)
	private RenderType renderType;

	private static PaletteBlockPattern create(String name, PatternNameType nameType,
		PaletteBlockPartial<?>... partials) {
		PaletteBlockPattern pattern = new PaletteBlockPattern();
		pattern.id = name;
		pattern.ctFactory = Optional.empty();
		pattern.nameType = nameType;
		pattern.partials = partials;
		pattern.additionalRecipes = $ -> NonNullBiConsumer.noop();
		pattern.isTranslucent = false;
		pattern.blockFactory = Block::new;
		pattern.textures = new String[] { name };
		pattern.blockStateGenerator = p -> p::cubeAll;
		return pattern;
	}

	public IPatternBlockStateGenerator getBlockStateGenerator() {
		return blockStateGenerator;
	}

	public boolean isTranslucent() {
		return isTranslucent;
	}

	public TagKey<Block>[] getBlockTags() {
		return blockTags;
	}

	public TagKey<Item>[] getItemTags() {
		return itemTags;
	}

	public NonNullFunction<Properties, ? extends Block> getBlockFactory() {
		return blockFactory;
	}

	public PaletteBlockPartial<? extends Block>[] getPartials() {
		return partials;
	}

	public String getTexture(int index) {
		return textures[index];
	}

	public void addRecipes(NonNullSupplier<Block> baseBlock, DataGenContext<Block, ? extends Block> c,
		RegistrateRecipeProvider p) {
		additionalRecipes.apply(baseBlock)
			.accept(c, p);
	}

	public Optional<Supplier<ConnectedTextureBehaviour>> createCTBehaviour(String variant) {
		return ctFactory.map(d -> () -> d.apply(variant));
	}

	// Builder

	private PaletteBlockPattern blockStateFactory(IPatternBlockStateGenerator factory) {
		blockStateGenerator = factory;
		return this;
	}

	private PaletteBlockPattern textures(String... textures) {
		this.textures = textures;
		return this;
	}

	private PaletteBlockPattern block(NonNullFunction<Properties, ? extends Block> blockFactory) {
		this.blockFactory = blockFactory;
		return this;
	}

	private PaletteBlockPattern connectedTextures(Function<String, ConnectedTextureBehaviour> factory) {
		this.ctFactory = Optional.of(factory);
		return this;
	}

	// Model generators

	public IBlockStateProvider cubeAll(String variant) {
		Material all = new Material(toLocation(variant, textures[0]));
		return (ctx, prov) -> prov.generateWithTemplate(ctx.get(), ModelTemplates.CUBE_ALL,
			TextureMapping.cube(all));
	}

	public IBlockStateProvider cubeBottomTop(String variant) {
		Material side = new Material(toLocation(variant, textures[0]));
		Material bottom = new Material(toLocation(variant, textures[1]));
		Material top = new Material(toLocation(variant, textures[2]));
		return (ctx, prov) -> prov.generateWithTemplate(ctx.get(), ModelTemplates.CUBE_BOTTOM_TOP,
			new TextureMapping().put(TextureSlot.SIDE, side)
				.put(TextureSlot.BOTTOM, bottom)
				.put(TextureSlot.TOP, top)
				.put(TextureSlot.PARTICLE, side));
	}

	/**
	 * The upright pillar keeps the block's own model; the two horizontal axes share a second model that
	 * is tipped onto its side, so the cap only ever faces along the pillar.
	 */
	public IBlockStateProvider pillar(String variant) {
		Material side = new Material(toLocation(variant, textures[0]));
		Material end = new Material(toLocation(variant, textures[1]));

		return (ctx, prov) -> {
			TextureMapping textures = TextureMapping.column(side, end);
			MultiVariant upright = BlockModelGenerators
				.plainVariant(ModelTemplates.CUBE_COLUMN.create(ctx.get(), textures, prov.modelOutput));
			MultiVariant horizontal = BlockModelGenerators.plainVariant(
				ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(ctx.get(), textures, prov.modelOutput));

			prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get())
				.with(PropertyDispatch.initial(BlockStateProperties.AXIS)
					.generate(axis -> axis == Axis.Y ? upright
						: BlockStateGen.rotateY(BlockStateGen.rotateX(horizontal, 90),
							axis == Axis.X ? 90 : 0))));
		};
	}

	public IBlockStateProvider cubeColumn(String variant) {
		Material side = new Material(toLocation(variant, textures[0]));
		Material end = new Material(toLocation(variant, textures[1]));
		return (ctx, prov) -> prov.generateWithTemplate(ctx.get(), ModelTemplates.CUBE_COLUMN,
			TextureMapping.column(side, end));
	}

	// Utility

	protected String createName(String variant) {
		if (nameType == WRAP) {
			String[] split = id.split("_");
			if (split.length == 2) {
				String formatString = "%s_%s_%s";
				return String.format(formatString, split[0], variant, split[1]);
			}
		}
		String formatString = "%s_%s";
		return nameType == SUFFIX ? String.format(formatString, variant, id) : String.format(formatString, id, variant);
	}

	protected static Identifier toLocation(String variant, String texture) {
		return Create.asResource(
			String.format(TEXTURE_LOCATION, texture, variant + (texture.equals("cut") ? "_" : "_cut_") + texture));
	}

	protected static CTSpriteShiftEntry ct(String variant, CTs texture) {
		Identifier resLoc = texture.srcFactory.apply(variant);
		Identifier resLocTarget = texture.targetFactory.apply(variant);
		return CTSpriteShifter.getCT(texture.type, resLoc,
			Identifier.fromNamespaceAndPath(resLocTarget.getNamespace(), resLocTarget.getPath() + "_connected"));
	}

	@FunctionalInterface
	static interface IPatternBlockStateGenerator
		extends Function<PaletteBlockPattern, Function<String, IBlockStateProvider>> {
	}

	@FunctionalInterface
	static interface IBlockStateProvider
		extends NonNullBiConsumer<DataGenContext<Block, ? extends Block>, RegistrateBlockModelGenerator> {
	}

	enum PatternNameType {
		PREFIX, SUFFIX, WRAP
	}

	// Textures with connectability, used by Spriteshifter

	public enum CTs {

		PILLAR(AllCTTypes.RECTANGLE, s -> toLocation(s, "pillar")),
		CAP(AllCTTypes.OMNIDIRECTIONAL, s -> toLocation(s, "cap")),
		LAYERED(AllCTTypes.HORIZONTAL_KRYPPERS, s -> toLocation(s, "layered"))

		;

		public CTType type;
		private Function<String, Identifier> srcFactory;
		private Function<String, Identifier> targetFactory;

		private CTs(CTType type, Function<String, Identifier> factory) {
			this(type, factory, factory);
		}

		private CTs(CTType type, Function<String, Identifier> srcFactory,
			Function<String, Identifier> targetFactory) {
			this.type = type;
			this.srcFactory = srcFactory;
			this.targetFactory = targetFactory;
		}

	}

}
