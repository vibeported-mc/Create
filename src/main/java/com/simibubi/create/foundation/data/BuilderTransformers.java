package com.simibubi.create.foundation.data;

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
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.neoforged.neoforge.common.Tags;

public class BuilderTransformers {
	public static <B extends EncasedShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedShaft(String casing,
																										 Supplier<CTSpriteShiftEntry> casingShift) {
		return builder -> encasedBase(builder, () -> AllBlocks.SHAFT.get())
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())))
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
				(s, f) -> f.getAxis() != s.getValue(EncasedShaftBlock.AXIS))))
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	public static <B extends StandardBogeyBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> bogey() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
			.properties(p -> p.noOcclusion())
			.transform(pickaxeOnly())
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((p, l) -> p.dropOther(l, AllBlocks.RAILWAY_CASING.get()))
			
			.onRegister(
				block -> AbstractBogeyBlock.registerStandardBogey(RegisteredObjectsHelper.getKeyOrThrow(block)));
	}

	public static <B extends CopycatBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycat() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.mapColor(MapColor.NONE)
				.isValidSpawn((state, level, pos, type) -> false))
			.color(() -> CopycatBlock::wrappedColor)
			.transform(TagGen.axeOrPickaxe());
	}

	public static <B extends TrapDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> trapdoor(boolean orientable) {
		return b -> b/* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.transform(pickaxeOnly())
			.onRegister(interactionBehaviour(new DoorMovingInteraction()))
			.onRegister(movementBehaviour(new SlidingDoorMovementBehaviour()))
			.tag(BlockItemTags.DOORS.block())
			.tag(BlockItemTags.WOODEN_DOORS.block()) // for villager AI
			.tag(AllBlockTags.NON_DOUBLE_DOOR.tag)
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((lr, block) -> lr.add(block, lr.createDoorTable(block)))
			
			.item()
			.tag(BlockItemTags.DOORS.item())
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	private static <B extends RotatedPillarKineticBlock, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b,
																						   Supplier<ItemLike> drop) {
		return b.initialProperties(SharedProperties::stone)
			.properties(BlockBehaviour.Properties::noOcclusion)
			.transform(CStress.setNoImpact())
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((p, lb) -> p.dropOther(lb, drop.get()))
			;
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cuckooClock() {
		return b -> b.initialProperties(SharedProperties::wooden)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.transform(CStress.setImpact(1))
			.item()
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .transform(ModelGen.customItemModel("cuckoo_clock", "item"))
			.build()
			;
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> ladder(String name,
																					   Supplier<DataIngredient> ingredient, MapColor color) {
		return b -> b.initialProperties(() -> Blocks.LADDER)
			.properties(p -> p.mapColor(color))
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.properties(p -> p.sound(SoundType.COPPER))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item()
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
			
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> scaffold(String name,
																						 Supplier<DataIngredient> ingredient, MapColor color, CTSpriteShiftEntry scaffoldShift,
																						 CTSpriteShiftEntry scaffoldInsideShift, CTSpriteShiftEntry casingShift) {
		return b -> b.initialProperties(() -> Blocks.SCAFFOLDING)
			.properties(p -> p.sound(SoundType.COPPER)
				.mapColor(color))
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.onRegister(connectedTextures(
				() -> new MetalScaffoldingCTBehaviour(scaffoldShift, scaffoldInsideShift, casingShift)))
			.transform(pickaxeOnly())
			.tag(BlockTags.CLIMBABLE)
			.item(MetalScaffoldingBlockItem::new)
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
			
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	public static <B extends ValveHandleBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> valveHandle(
		@Nullable DyeColor color) {
		return b -> b.initialProperties(SharedProperties::copperMetal)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item(BeltTunnelItem::new)
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mechanicalPiston(PistonType type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.properties(p -> p.noOcclusion())
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.transform(CStress.setImpact(4.0))
			.item()
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .transform(ModelGen.customItemModel("mechanical_piston", type.getSerializedName(), "item"))
			.build()
			;
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
		return b -> b.initialProperties(SharedProperties::stone)
			.transform(axeOrPickaxe())
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			.properties(p -> type.equals("creative") ? p.rarity(Rarity.EPIC) : p)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// .transform(ModelGen.customItemModel("crate", type, "single"))
			.build()
			;
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> backtank(Supplier<ItemLike> drop) {
		return b -> b/* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			.transform(pickaxeOnly())
			.transform(CStress.setImpact(4.0))
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .loot((lt, block) -> {
				// Builder builder = LootTable.lootTable();
				// LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
				// lt.add(block, builder.withPool(LootPool.lootPool()
					// .when(survivesExplosion)
					// .setRolls(ConstantValue.exactly(1))
					// .add(LootItem.lootTableItem(drop.get())
						// .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
							// .include(AllDataComponents.BACKTANK_AIR)))));
			// })
			;
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bell() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.noOcclusion()
				.sound(SoundType.ANVIL))
			.transform(pickaxeOnly())
			.tag(AllBlockTags.BRITTLE.tag)
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.build();
	}

	public static ItemBuilder<PackageItem, CreateRegistrate> packageItem(PackageStyle style) {
		String size = "_" + style.width() + "x" + style.height();
		return Create.registrate().item(style.getItemId()
				.getPath(), p -> new PackageItem(p, style))
			.properties(p -> p.stacksTo(1))
			.tag(AllItemTags.PACKAGES.tag)
			// TODO 26.2: port datagen to RegistrateItemModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
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
				// TODO 26.2: port datagen to RegistrateBlockModelGenerator
				// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
				
				
				.onRegister(CreateRegistrate.blockModel(() -> TableClothModel::new))
				.tag(AllBlockTags.TABLE_CLOTHS.tag, soundTag)
				.onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.table_cloth"))
				.item(TableClothBlockItem::new);

			if (dyed)
				item.tag(AllItemTags.DYED_TABLE_CLOTHS.tag);

			return item/* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
				.tag(AllItemTags.TABLE_CLOTHS.tag)
				// TODO 26.2: port datagen to the new recipe/loot builders
				// .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
					// .requires(c.get())
					// .unlockedBy("has_" + c.getName(), RegistrateRecipeProvider.has(c.get()))
					// .save(p, Create.asResource("crafting/logistics/" + c.getName() + "_clear")))
				
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
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.item()
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			.build();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> palettesIronBlock() {
		return b -> b.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
				.sound(SoundType.NETHERITE_BLOCK)
				.requiresCorrectToolForDrops())
			.transform(pickaxeOnly())
			// TODO 26.2: port datagen to RegistrateBlockModelGenerator
			// /* TODO 26.2: port datagen to RegistrateBlockModelGenerator */
			
			
			.tag(AllBlockTags.WRENCH_PICKUP.tag)
			// TODO 26.2: port datagen to the new recipe/loot builders
			// .recipe((c, p) -> p.stonecutting(DataIngredient.tag(BuiltInRegistries.ITEM.getOrThrow(Tags.Items.INGOTS_IRON)), RecipeCategory.BUILDING_BLOCKS,
				// c::get, 2))
			
			.simpleItem();
	}
}
