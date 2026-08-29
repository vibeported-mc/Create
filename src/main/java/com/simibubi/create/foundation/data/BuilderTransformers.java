package com.simibubi.create.foundation.data;

import java.util.Optional;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;

import com.simibubi.create.content.contraptions.piston.MechanicalPistonGenerator;
import com.simibubi.create.content.logistics.packager.PackagerGenerator;

import net.minecraft.tags.BlockItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlockItem;
import com.simibubi.create.content.decoration.MetalScaffoldingCTBehaviour;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockItem;
import com.simibubi.create.content.logistics.tableCloth.TableClothModel;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock.Shape;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelItem;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.neoforged.neoforge.common.Tags;

public class BuilderTransformers {

	private static final TextureSlot CASING = TextureSlot.create("casing");
	// Several models name their textures by number. A template and its texture mapping have to hand
	// around the same TextureSlot instance, which is compared by identity, so keep one of each.
	private static final TextureSlot SLOT_0 = TextureSlot.create("0");
	private static final TextureSlot SLOT_1 = TextureSlot.create("1");
	private static final TextureSlot SLOT_2 = TextureSlot.create("2");
	private static final TextureSlot SLOT_3 = TextureSlot.create("3");
	private static final TextureSlot SLOT_4 = TextureSlot.create("4");
	private static final TextureSlot INSIDE = TextureSlot.create("inside");
	private static final TextureSlot TUNNEL = TextureSlot.create("tunnel");
	private static final TextureSlot DIRECTION = TextureSlot.create("direction");
	private static final TextureSlot FRAME = TextureSlot.create("frame");
	private static final TextureSlot BACK = TextureSlot.create("back");
	private static final TextureSlot CRATE = TextureSlot.create("crate");
	public static <B extends EncasedShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedShaft(String casing,
																										 Supplier<CTSpriteShiftEntry> casingShift) {
		return builder -> encasedBase(builder, () -> AllBlocks.SHAFT.get())
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())))
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
				(s, f) -> f.getAxis() != s.getValue(EncasedShaftBlock.AXIS))))
			.blockstate(() -> (c, p) -> axisBlock(c, p, blockState -> BlockModelGenerators
				.plainVariant(p.modLoc("block/encased_shaft/block_" + casing)), true))
			.item()
			.model(() -> AssetLookup.customBlockItemModel("encased_shaft", "item_" + casing))
			.build();
	}

	public static <B extends StandardBogeyBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> bogey() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
			.properties(p -> p.noOcclusion())
			.transform(pickaxeOnly())
			.blockstate(() -> (c, p) -> BlockStateGen.horizontalAxisBlock(c, p,
				s -> BlockModelGenerators.plainVariant(p.modLoc("block/track/bogey/top"))))
			.loot((p, l) -> p.dropOther(l, AllBlocks.RAILWAY_CASING.get()))
			.onRegister(
				block -> AbstractBogeyBlock.registerStandardBogey(RegisteredObjectsHelper.getKeyOrThrow(block)));
	}

	public static <B extends CopycatBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycat() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.blockstate(() -> (c, p) -> p.blockStateOutput.accept(BlockModelGenerators
				.createSimpleBlock(c.get(), BlockModelGenerators.plainVariant(p.mcLoc("block/air")))))
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.mapColor(MapColor.NONE)
				.isValidSpawn((state, level, pos, type) -> false))
			.color(() -> CopycatBlock::wrappedColor)
			.transform(TagGen.axeOrPickaxe());
	}

	public static <B extends TrapDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> trapdoor(boolean orientable) {
		return b -> b.blockstate(() -> (c, p) -> {
				MultiVariant bottom = AssetLookup.partialBaseVariant(c, p, "bottom");
				MultiVariant top = AssetLookup.partialBaseVariant(c, p, "top");
				MultiVariant open = AssetLookup.partialBaseVariant(c, p, "open");
				if (orientable)
					BlockStateGen.orientableTrapdoorBlock(c.get(), AssetLookup.partialBaseModel(c, p, "bottom"),
						AssetLookup.partialBaseModel(c, p, "top"), AssetLookup.partialBaseModel(c, p, "open"))
						.accept(c, p);
				else
					BlockStateGen.uvLockedTrapdoorBlock(c.get(),
						AssetLookup.partialBaseModel(c, p, "bottom"), AssetLookup.partialBaseModel(c, p, "top"),
						AssetLookup.partialBaseModel(c, p, "open"))
						.accept(c, p);
			})
			.transform(pickaxeOnly())
			.tag(BlockItemTags.TRAPDOORS.block())
			.onRegister(interactionBehaviour(new TrapdoorMovingInteraction()))
			.item()
			.tag(BlockItemTags.TRAPDOORS.item())
			.build();
	}

	public static <B extends SlidingDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> slidingDoor(String type) {
		return b -> b.initialProperties(() -> Blocks.IRON_DOOR)
			.properties(p -> p.requiresCorrectToolForDrops()
				.strength(3.0F, 6.0F))
			// The sliding doors also carry a "visible" property, which the vanilla door dispatch does not
			// know about, so walk the states and work the turn out per state instead.
			.blockstate(() -> (c, p) -> BlockStateGen.forAllStates(c, p, state -> {
				MultiVariant variant = AssetLookup.partialBaseVariant(c, p,
					state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? "bottom" : "top");
				boolean open = state.getValue(BlockStateProperties.OPEN);
				boolean right = state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.RIGHT;
				int yRot = ((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING)
					.toYRot()) + 90 + (open ? 90 : 0) + (open && right ? 180 : 0);
				return BlockStateGen.rotateY(variant, yRot);
			}, BlockStateProperties.POWERED))
			.transform(pickaxeOnly())
			.onRegister(interactionBehaviour(new DoorMovingInteraction()))
			.onRegister(movementBehaviour(new SlidingDoorMovementBehaviour()))
			.tag(BlockItemTags.DOORS.block())
			.tag(BlockItemTags.WOODEN_DOORS.block()) // for villager AI
			.tag(AllBlockTags.NON_DOUBLE_DOOR.tag)
			.loot((lr, block) -> lr.add(block, lr.createDoorTable(block)))
			.item()
			.tag(BlockItemTags.DOORS.item())
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.model(() -> (c, p) -> p.generateFlatItem(c.getEntry(),
				p.modItemTexture(type + "_door")))
			.build();
	}

	public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedCogwheel(
		String casing, Supplier<CTSpriteShiftEntry> casingShift) {
		return b -> encasedCogwheelBase(b, casing, casingShift, () -> AllBlocks.COGWHEEL.get(), false);
	}

	public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLargeCogwheel(
		String casing, Supplier<CTSpriteShiftEntry> casingShift) {
		return b -> encasedCogwheelBase(b, casing, casingShift, () -> AllBlocks.LARGE_COGWHEEL.get(), true)
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(casingShift.get())));
	}

	private static <B extends EncasedCogwheelBlock, P> BlockBuilder<B, P> encasedCogwheelBase(BlockBuilder<B, P> b,
																							  String casing, Supplier<CTSpriteShiftEntry> casingShift, Supplier<ItemLike> drop, boolean large) {
		String encasedSuffix = "_encased_cogwheel_side" + (large ? "_connected" : "");
		String blockFolder = large ? "encased_large_cogwheel" : "encased_cogwheel";
		String wood = casing.equals("brass") ? "dark_oak" : "spruce";
		String gearbox = casing.equals("brass") ? "brass_gearbox" : "gearbox";
		return encasedBase(b, drop)
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
				(s, f) -> f.getAxis() == s.getValue(EncasedCogwheelBlock.AXIS)
					&& !s.getValue(f.getAxisDirection() == AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT
					: EncasedCogwheelBlock.BOTTOM_SHAFT))))
			.blockstate(() -> (c, p) -> axisBlock(c, p, blockState -> {
				String suffix = (blockState.getValue(EncasedCogwheelBlock.TOP_SHAFT) ? "_top" : "")
					+ (blockState.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT) ? "_bottom" : "");
				String modelName = c.getName() + suffix;
				return BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc("block/" + blockFolder + "/block" + suffix))
					.texture(CASING, new Material(Create.asResource("block/" + casing + "_casing")))
					.texture(TextureSlot.PARTICLE, new Material(Create.asResource("block/" + casing + "_casing")))
					.texture(SLOT_4, new Material(Create.asResource("block/" + gearbox)))
					.texture(SLOT_1,
						new Material(Identifier.withDefaultNamespace("block/stripped_" + wood + "_log_top")))
					.texture(TextureSlot.SIDE, new Material(Create.asResource("block/" + casing + encasedSuffix)))
					.build(p.modLoc("block/" + modelName)));
			}, false))
			.item()
			.model(() -> (c, p) -> {
				ModelTemplate template = new ModelTemplate(Optional.of(p.modLoc("block/" + blockFolder + "/item")),
					Optional.empty(), CASING, TextureSlot.PARTICLE, SLOT_1, TextureSlot.SIDE);
				p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
					.put(CASING, new Material(Create.asResource("block/" + casing + "_casing")))
					.put(TextureSlot.PARTICLE, new Material(Create.asResource("block/" + casing + "_casing")))
					.put(SLOT_1,
						new Material(Identifier.withDefaultNamespace("block/stripped_" + wood + "_log_top")))
					.put(TextureSlot.SIDE, new Material(Create.asResource("block/" + casing + encasedSuffix))));
			})
			.build();
	}

	private static <B extends RotatedPillarKineticBlock, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b,
																						   Supplier<ItemLike> drop) {
		return b.initialProperties(SharedProperties::stone)
			.properties(BlockBehaviour.Properties::noOcclusion)
			.transform(CStress.setNoImpact())
			.loot((p, lb) -> p.dropOther(lb, drop.get()));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cuckooClock() {
		return b -> b.initialProperties(SharedProperties::wooden)
			.blockstate(() -> (c, p) -> BlockStateGen.horizontalBlock(c, p,
				$ -> BlockModelGenerators.plainVariant(p.modLoc("block/cuckoo_clock/block"))))
			.transform(CStress.setImpact(1))
			.item()
			.transform(ModelGen.customItemModel("cuckoo_clock", "item"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> ladder(String name,
																					   Function<RegistrateRecipeProvider, DataIngredient> ingredient, MapColor color) {
		return b -> b.initialProperties(() -> Blocks.LADDER)
			.properties(p -> p.mapColor(color))
			.blockstate(() -> (c, p) -> BlockStateGen.horizontalBlock(c, p,
				$ -> BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc("block/ladder"))
					.texture(SLOT_0, new Material(p.modLoc("block/ladder_" + name + "_hoop")))
					.texture(SLOT_1, new Material(p.modLoc("block/ladder_" + name)))
					.texture(TextureSlot.PARTICLE, new Material(p.modLoc("block/ladder_" + name)))
					.build(p.modLoc("block/" + c.getName())))))
			.properties(p -> p.sound(SoundType.COPPER))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item()
			.recipe((c, p) -> p.stonecutting(ingredient.apply(p), RecipeCategory.DECORATIONS, c::get, 2))
			.model(() -> (c, p) -> p.generateFlatItem(c.getEntry(), p.modBlockTexture("ladder_" + name)))
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> scaffold(String name,
																						 Function<RegistrateRecipeProvider, DataIngredient> ingredient, MapColor color, CTSpriteShiftEntry scaffoldShift,
																						 CTSpriteShiftEntry scaffoldInsideShift, CTSpriteShiftEntry casingShift) {
		return b -> b.initialProperties(() -> Blocks.SCAFFOLDING)
			.properties(p -> p.sound(SoundType.COPPER)
				.mapColor(color))
			.blockstate(() -> (c, p) -> p.blockStateOutput.accept(MultiVariantGenerator.dispatch(c.get())
				.with(PropertyDispatch.initial(MetalScaffoldingBlock.BOTTOM)
					.generate(bottom -> {
						String suffix = bottom ? "_horizontal" : "";
						return BlockModelGenerators.plainVariant(p.getBuilder()
							.parent(p.modLoc("block/scaffold/block" + suffix))
							.texture(TextureSlot.TOP, new Material(p.modLoc("block/funnel/" + name + "_funnel_frame")))
							.texture(INSIDE, new Material(p.modLoc("block/scaffold/" + name + "_scaffold_inside")))
							.texture(TextureSlot.SIDE, new Material(p.modLoc("block/scaffold/" + name + "_scaffold")))
							.texture(CASING, new Material(p.modLoc("block/" + name + "_casing")))
							.texture(TextureSlot.PARTICLE,
								new Material(p.modLoc("block/scaffold/" + name + "_scaffold")))
							.build(p.modLoc("block/" + c.getName() + suffix)));
					}))))
			.onRegister(connectedTextures(
				() -> new MetalScaffoldingCTBehaviour(scaffoldShift, scaffoldInsideShift, casingShift)))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item(MetalScaffoldingBlockItem::new)
			.recipe((c, p) -> p.stonecutting(ingredient.apply(p), RecipeCategory.DECORATIONS, c::get, 2))
			.model(() -> (c, p) -> p.createWithExistingModel(c.getEntry(), p.modLoc("block/" + c.getName())))
			.build();
	}

	public static <B extends ValveHandleBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> valveHandle(
		@Nullable DyeColor color) {
		return b -> b.initialProperties(SharedProperties::copperMetal)
			.blockstate(() -> (c, p) -> {
				String variant = color == null ? "copper" : color.getSerializedName();
				BlockStateGen.directionalBlock(c, p, $ -> BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc("block/valve_handle"))
					.texture(SLOT_3,
						new Material(p.modLoc("block/valve_handle/valve_handle_" + variant)))
					.build(p.modLoc("block/" + variant + "_valve_handle"))));
			})
			.tag(AllBlockTags.BRITTLE.tag, AllBlockTags.VALVE_HANDLES.tag)
			.onRegister(BlockStressValues.setGeneratorSpeed(32))
			.onRegister(ItemUseOverrides::addBlock)
			.item()
			.tag(AllItemTags.VALVE_HANDLES.tag)
			.build();
	}

	public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> casing(
		Supplier<CTSpriteShiftEntry> ct) {
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.sound(SoundType.WOOD))
			.transform(axeOrPickaxe())
			.blockstate(() -> (c, p) -> p.createTrivialCube(c.get()))
			.onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get())))
			.onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get())))
			.tag(AllBlockTags.CASING.tag)
			.item()
			.tag(AllItemTags.CASING.tag)
			.build();
	}

	public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> layeredCasing(
		Supplier<CTSpriteShiftEntry> ct, Supplier<CTSpriteShiftEntry> ct2) {
		return b -> b.initialProperties(SharedProperties::stone)
			.transform(axeOrPickaxe())
			.blockstate(() -> (c, p) -> p.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(c.get(),
				BlockModelGenerators.plainVariant(p.withParent(ModelTemplates.CUBE_COLUMN,
					new TextureMapping()
						.put(TextureSlot.SIDE, new Material(ct.get()
							.getOriginalIdentifier()))
						.put(TextureSlot.END, new Material(ct2.get()
							.getOriginalIdentifier())))
					.build(p.modLoc("block/" + c.getName()))))))
			.onRegister(connectedTextures(() -> new HorizontalCTBehaviour(ct.get(), ct2.get())))
			.onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get())))
			.tag(AllBlockTags.CASING.tag)
			.item()
			.tag(AllItemTags.CASING.tag)
			.build();
	}

	public static <B extends BeltTunnelBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> beltTunnel(
		String type, Identifier particleTexture) {
		String prefix = "block/tunnel/" + type + "_tunnel";
		String funnel_prefix = "block/funnel/" + type + "_funnel";
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(BlockBehaviour.Properties::noOcclusion)
			.transform(pickaxeOnly())
			.blockstate(() -> (c, p) -> p.blockStateOutput.accept(MultiVariantGenerator.dispatch(c.get())
				.with(PropertyDispatch.initial(BeltTunnelBlock.SHAPE, BeltTunnelBlock.HORIZONTAL_AXIS)
					.generate((shape, axis) -> {
						String window = shape == Shape.WINDOW ? "_window" : "";
						Shape modelShape = shape == Shape.CLOSED ? Shape.STRAIGHT : shape;
						String shapeName = modelShape.getSerializedName();
						MultiVariant variant = BlockModelGenerators.plainVariant(p.getBuilder()
							.parent(p.modLoc("block/belt_tunnel/" + shapeName))
							.texture(TextureSlot.TOP, new Material(p.modLoc(prefix + "_top" + window)))
							.texture(TUNNEL, new Material(p.modLoc(prefix)))
							.texture(DIRECTION, new Material(p.modLoc(funnel_prefix + "_neutral")))
							.texture(FRAME, new Material(p.modLoc(funnel_prefix + "_frame")))
							.texture(TextureSlot.PARTICLE, new Material(particleTexture))
							.build(p.modLoc(prefix + "/" + shapeName)));
						return BlockStateGen.rotateY(variant, axis == Axis.X ? 0 : 90);
					}))))
			.item(BeltTunnelItem::new)
			.model(() -> (c, p) -> {
				ModelTemplate template = new ModelTemplate(Optional.of(p.modLoc("block/belt_tunnel/item")),
					Optional.empty(), TextureSlot.TOP, TUNNEL, DIRECTION, FRAME, TextureSlot.PARTICLE);
				p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
					.put(TextureSlot.TOP, new Material(p.modLoc(prefix + "_top")))
					.put(TUNNEL, new Material(p.modLoc(prefix)))
					.put(DIRECTION, new Material(p.modLoc(funnel_prefix + "_neutral")))
					.put(FRAME, new Material(p.modLoc(funnel_prefix + "_frame")))
					.put(TextureSlot.PARTICLE, new Material(particleTexture)));
			})
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mechanicalPiston(PistonType type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.noOcclusion())
			.blockstate(() -> new MechanicalPistonGenerator(type)::generate)
			.transform(CStress.setImpact(4.0))
			.item()
			.transform(ModelGen.customItemModel("mechanical_piston", type.getSerializedName(), "item"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bearing(String prefix,
																						String backTexture) {
		Identifier baseBlockModelLocation = Create.asResource("block/bearing/block");
		Identifier baseItemModelLocation = Create.asResource("block/bearing/item");
		Identifier topTextureLocation = Create.asResource("block/bearing_top");
		Identifier sideTextureLocation = Create.asResource("block/" + prefix + "_bearing_side");
		Identifier backTextureLocation = Create.asResource("block/" + backTexture);
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.noOcclusion())
			.blockstate(() -> (c, p) -> BlockStateGen.directionalBlock(c, p,
				$ -> BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(baseBlockModelLocation)
					.texture(TextureSlot.SIDE, new Material(sideTextureLocation))
					.texture(BACK, new Material(backTextureLocation))
					.build(p.modLoc("block/" + c.getName())))))
			.item()
			.model(() -> (c, p) -> {
				ModelTemplate template = new ModelTemplate(Optional.of(baseItemModelLocation), Optional.empty(),
					TextureSlot.TOP, TextureSlot.SIDE, BACK);
				p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
					.put(TextureSlot.TOP, new Material(topTextureLocation))
					.put(TextureSlot.SIDE, new Material(sideTextureLocation))
					.put(BACK, new Material(backTextureLocation)));
			})
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.transform(axeOrPickaxe())
			.blockstate(() -> (c, p) -> {
				Identifier crate = p.modLoc("block/crate_" + type);
				Identifier side = p.modLoc("block/crate_" + type + "_side");
				Identifier casing = p.modLoc("block/" + type + "_casing");

				// The four halves are written for the sake of the resource pack; every state resolves to
				// the single crate, which is what the block model itself swaps out at runtime.
				Identifier single = null;
				for (String variant : new String[] { "single", "top", "bottom", "left", "right" }) {
					Identifier model = p.getBuilder()
						.parent(p.modLoc("block/crate/" + variant))
						.texture(CRATE, new Material(crate))
						.texture(TextureSlot.SIDE, new Material(side))
						.texture(CASING, new Material(casing))
						.build(p.modLoc("block/crate/" + type + "/" + variant));
					if (variant.equals("single"))
						single = model;
				}

				MultiVariant variant = BlockModelGenerators.plainVariant(single);
				BlockStateGen.forAllStates(c, p, $ -> variant);
			})
			.item()
			.properties(p -> type.equals("creative") ? p.rarity(Rarity.EPIC) : p)
			.transform(ModelGen.customItemModel("crate", type, "single"));
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> backtank(Supplier<ItemLike> drop) {
		return b -> b
			.blockstate(() -> (c, p) -> BlockStateGen.horizontalBlock(c, p,
				$ -> AssetLookup.partialBaseVariant(c, p)))
			.transform(pickaxeOnly())
			.transform(CStress.setImpact(4.0))
			.loot((lt, block) -> {
				Builder builder = LootTable.lootTable();
				LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
				lt.add(block, builder.withPool(LootPool.lootPool()
					.when(survivesExplosion)
					.setRolls(ConstantValue.exactly(1))
					.add(LootItem.lootTableItem(drop.get())
						.apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
							.include(AllDataComponents.BACKTANK_AIR)))));
			});
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bell() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.sound(SoundType.ANVIL))
			.transform(pickaxeOnly())
			.tag(AllBlockTags.BRITTLE.tag)
			.blockstate(() -> (c, p) -> BlockStateGen.horizontalBlock(c, p, state -> {
				String variant = state.getValue(BlockStateProperties.BELL_ATTACHMENT)
					.getSerializedName();
				return BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc("block/bell_base/block_" + variant))
					.build(p.modLoc("block/" + c.getName() + "_" + variant)));
			}))
			.item()
			.model(() -> (c, p) -> p.createWithExistingModel(c.getEntry(), p.modLoc("block/" + c.getName())))
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.build();
	}

	public static ItemBuilder<PackageItem, CreateRegistrate> packageItem(PackageStyle style) {
		String size = "_" + style.width() + "x" + style.height();
		return Create.registrate().item(style.getItemId()
				.getPath(), p -> new PackageItem(p, style))
			.properties(p -> p.stacksTo(1))
			// Every package of a kind shares one name. getDescriptionId is final in 26.2, so the id is
			// set on the properties rather than overridden on the item.
			.properties(p -> p.overrideDescription("item." + Create.ID + (style.rare() ? ".rare_package"
				: ".package")))
			.tag(AllItemTags.PACKAGES.tag)
			.model(() -> (c, p) -> {
				if (style.rare())
					p.generateWithTemplate(c.getEntry(),
						new ModelTemplate(Optional.of(p.modLoc("item/package/custom" + size)), Optional.empty(),
							SLOT_2),
						new TextureMapping().put(SLOT_2,
							new Material(p.modLoc("block/package/" + style.type()))));
				else
					p.createWithExistingModel(c.getEntry(), p.modLoc("item/package/" + style.type() + size));
			})
			.lang((style.rare() ? "Rare"
				: style.type()
				.substring(0, 1)
				.toUpperCase(Locale.ROOT)
				+ style.type()
				.substring(1))
				+ " Package");
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> tableCloth(String name,
																						   NonNullSupplier<? extends Block> initialProps, boolean dyed) {
		return b -> {
			TagKey<Block> soundTag = dyed ? BlockTags.COMBINATION_STEP_SOUND_BLOCKS : BlockTags.INSIDE_STEP_SOUND_BLOCKS;

			ItemBuilder<TableClothBlockItem, BlockBuilder<B, P>> item = b.initialProperties(initialProps)
				.blockstate(() -> (c, p) -> p.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(c.get(),
					BlockModelGenerators.plainVariant(p.getBuilder()
						.parent(p.modLoc("block/table_cloth/block"))
						.texture(SLOT_0, new Material(p.modLoc("block/table_cloth/" + name)))
						.build(p.modLoc("block/" + name + "_table_cloth"))))))
				.onRegister(CreateRegistrate.blockModel(() -> TableClothModel::new))
				.tag(AllBlockTags.TABLE_CLOTHS.tag, soundTag)
				.onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.table_cloth"))
				.item(TableClothBlockItem::new);

			if (dyed)
				item.tag(AllItemTags.DYED_TABLE_CLOTHS.tag);

			return item
				.model(() -> (c, p) -> p.generateWithTemplate(c.getEntry(),
					new ModelTemplate(Optional.of(p.modLoc("block/table_cloth/item")), Optional.empty(), SLOT_0),
					new TextureMapping().put(SLOT_0, new Material(p.modLoc("block/table_cloth/" + name)))))
				.tag(AllItemTags.TABLE_CLOTHS.tag)
				.recipe((c, p) -> p.shapeless(RecipeCategory.MISC, c.get())
					.requires(c.get())
					.unlockedBy("has_" + c.getName(), p.has(c.get()))
					.save(p, Create.asResource("crafting/logistics/" + c.getName() + "_clear")
						.toString()))
				.build();
		};
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> packager() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion())
			.properties(p -> p.isRedstoneConductor(($1, $2, $3) -> false))
			.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
				.sound(SoundType.NETHERITE_BLOCK))
			.transform(pickaxeOnly())
			.blockstate(() -> new PackagerGenerator()::generate)
			.item()
			.model(() -> AssetLookup::customItemModel)
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> palettesIronBlock() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
				.sound(SoundType.NETHERITE_BLOCK)
				.requiresCorrectToolForDrops())
			.transform(pickaxeOnly())
			.blockstate(() -> (c, p) -> p.generateWithTemplate(c.get(), ModelTemplates.CUBE_COLUMN,
				TextureMapping.column(p.modBlockTexture(c.getName()), p.modBlockTexture(c.getName() + "_top"))))
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.recipe((c, p) -> p.stonecutting(DataIngredient.tag(p.itemLookup()
				.getOrThrow(Tags.Items.INGOTS_IRON)), RecipeCategory.BUILDING_BLOCKS, c::get, 2))
			.simpleItem();
	}
}
