
package com.simibubi.create.foundation.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.chassis.LinearChassisBlock;
import com.simibubi.create.content.contraptions.chassis.RadialChassisBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock.WhistleExtenderShape;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.Pointing;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Shared blockstate shapes, on 26.2's model datagen.
 * <p>
 * A blockstate is no longer a predicate mapped across every state: it dispatches over the properties
 * it actually varies with, so properties left out - {@code WATERLOGGED}, mostly - are ignored rather
 * than excluded by name. Rotations are quadrant mutators applied to a {@link MultiVariant} instead of
 * angles in degrees, which means only multiples of 90 can be expressed; the places that used to
 * compute an arbitrary angle now select from the four quadrants.
 */
public class BlockStateGen {

	// Functions

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> axisBlockProvider(
		boolean customItem) {
		return (c, p) -> axisBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> directionalBlockProvider(
		boolean customItem) {
		return (c, p) -> directionalBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> directionalBlockProviderIgnoresWaterlogged(
		boolean customItem) {
		return (c, p) -> directionalBlockIgnoresWaterlogged(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> horizontalBlockProvider(
		boolean customItem) {
		return (c, p) -> horizontalBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> horizontalAxisBlockProvider(
		boolean customItem) {
		return (c, p) -> horizontalAxisBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> simpleCubeAll(
		String path) {
		return (c, p) -> p.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(c.get(),
			BlockModelGenerators.plainVariant(cubeAllModel(p, c.getName(), p.modLoc("block/" + path)))));
	}

	public static <T extends DirectionalAxisKineticBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> directionalAxisBlockProvider() {
		return (c, p) -> directionalAxisBlock(c, p, ($, vertical) -> BlockModelGenerators.plainVariant(
			p.modLoc("block/" + c.getName() + "/" + (vertical ? "vertical" : "horizontal"))));
	}

	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator> horizontalWheelProvider(
		boolean customItem) {
		return (c, p) -> horizontalWheel(c, p, getBlockModel(customItem, c, p));
	}

	// Utility

	private static <T extends Block> Function<BlockState, MultiVariant> getBlockModel(boolean customItem,
		DataGenContext<Block, T> c, RegistrateBlockModelGenerator p) {
		return $ -> customItem ? AssetLookup.partialBaseVariant(c, p) : AssetLookup.standardVariant(c, p);
	}

	/**
	 * Turns a rotation in degrees into the mutator that applies it. Only right angles survive the
	 * trip, which is all a blockstate could ever express.
	 */
	public static MultiVariant rotateY(MultiVariant variant, int degrees) {
		return switch (Math.floorMod(degrees, 360)) {
			case 90 -> variant.with(BlockModelGenerators.Y_ROT_90);
			case 180 -> variant.with(BlockModelGenerators.Y_ROT_180);
			case 270 -> variant.with(BlockModelGenerators.Y_ROT_270);
			default -> variant;
		};
	}

	public static MultiVariant rotateX(MultiVariant variant, int degrees) {
		return switch (Math.floorMod(degrees, 360)) {
			case 90 -> variant.with(BlockModelGenerators.X_ROT_90);
			case 180 -> variant.with(BlockModelGenerators.X_ROT_180);
			case 270 -> variant.with(BlockModelGenerators.X_ROT_270);
			default -> variant;
		};
	}

	/**
	 * The same pair of turns as {@link #rotateX} and {@link #rotateY}, but as a mutator so it can be
	 * handed to a {@link PropertyDispatch#modify} dispatch rather than applied to one variant.
	 */
	public static VariantMutator rotationMutator(int xDegrees, int yDegrees) {
		VariantMutator x = switch (Math.floorMod(xDegrees, 360)) {
			case 90 -> BlockModelGenerators.X_ROT_90;
			case 180 -> BlockModelGenerators.X_ROT_180;
			case 270 -> BlockModelGenerators.X_ROT_270;
			default -> BlockModelGenerators.NOP;
		};
		VariantMutator y = switch (Math.floorMod(yDegrees, 360)) {
			case 90 -> BlockModelGenerators.Y_ROT_90;
			case 180 -> BlockModelGenerators.Y_ROT_180;
			case 270 -> BlockModelGenerators.Y_ROT_270;
			default -> BlockModelGenerators.NOP;
		};
		return x.then(y);
	}

	private static Identifier cubeAllModel(RegistrateBlockModelGenerator prov, String name, Identifier texture) {
		return prov.withParent(ModelTemplates.CUBE_ALL, TextureMapping.cube(new Material(texture)))
			.build(prov.modLoc("block/" + name));
	}

	// Generators

	/**
	 * The old model generators walked every blockstate and asked for a model per state. 26.2 wants a
	 * dispatch over named properties instead, so cover every property the block has - bar the ones the
	 * caller says to leave out - and hand the model function the state each combination stands for.
	 */
	public static <T extends Block> void forAllStates(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc,
		Property<?>... ignored) {
		T block = ctx.getEntry();
		List<Property<?>> properties = new ArrayList<>(block.getStateDefinition()
			.getProperties());
		properties.removeAll(List.of(ignored));

		if (properties.isEmpty()) {
			prov.blockStateOutput.accept(
				BlockModelGenerators.createSimpleBlock(block, modelFunc.apply(block.defaultBlockState())));
			return;
		}

		AllStatesDispatch dispatch = new AllStatesDispatch(properties);
		fillStates(dispatch, properties, 0, PropertyValueList.EMPTY, block.defaultBlockState(), modelFunc);
		prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
			.with(dispatch));
	}

	private static void fillStates(AllStatesDispatch dispatch, List<Property<?>> properties, int index,
		PropertyValueList key, BlockState state, Function<BlockState, MultiVariant> modelFunc) {
		if (index == properties.size()) {
			dispatch.put(key, modelFunc.apply(state));
			return;
		}
		for (Property.Value<?> value : properties.get(index)
			.getAllValues()
			.toList())
			fillStates(dispatch, properties, index + 1, key.extend(value), setValue(state, value), modelFunc);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static BlockState setValue(BlockState state, Property.Value<?> value) {
		return state.setValue((Property) value.property(), (Comparable) value.value());
	}

	/**
	 * A dispatch whose properties are only known at runtime, so that {@link #forAllStates} can cover
	 * whatever a block happens to declare.
	 */
	private static class AllStatesDispatch extends PropertyDispatch<MultiVariant> {

		private final List<Property<?>> properties;

		private AllStatesDispatch(List<Property<?>> properties) {
			this.properties = List.copyOf(properties);
		}

		private void put(PropertyValueList key, MultiVariant variant) {
			putValue(key, variant);
		}

		@Override
		public List<Property<?>> getDefinedProperties() {
			return properties;
		}
	}

	public static <T extends Block> void directionalBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov,
			state -> facingVariant(modelFunc.apply(state), state.getValue(BlockStateProperties.FACING)));
	}

	public static <T extends Block> void directionalBlockIgnoresWaterlogged(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov,
			state -> facingVariant(modelFunc.apply(state), state.getValue(BlockStateProperties.FACING)),
			BlockStateProperties.WATERLOGGED);
	}

	private static MultiVariant facingVariant(MultiVariant variant, Direction dir) {
		variant = rotateX(variant, dir == Direction.DOWN ? 180
			: dir.getAxis()
				.isHorizontal() ? 90 : 0);
		return rotateY(variant, dir.getAxis()
			.isVertical() ? 0 : ((int) dir.toYRot()) + 180);
	}

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		Function<BlockState, MultiVariant> modelFunc) {
		axisBlock(ctx, prov, modelFunc, false);
	}

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		Function<BlockState, MultiVariant> modelFunc, boolean uvLock) {
		forAllStates(ctx, prov, state -> {
			Axis axis = state.getValue(BlockStateProperties.AXIS);
			MultiVariant variant = modelFunc.apply(state);
			if (uvLock)
				variant = variant.with(BlockModelGenerators.UV_LOCK);
			variant = rotateX(variant, axis == Axis.Y ? 0 : 90);
			return rotateY(variant, axis == Axis.X ? 90 : axis == Axis.Z ? 180 : 0);
		}, BlockStateProperties.WATERLOGGED);
	}

	public static <T extends Block> void simpleBlock(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, modelFunc, BlockStateProperties.WATERLOGGED);
	}

	public static <T extends Block> void horizontalBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, state -> rotateY(modelFunc.apply(state),
			((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING)
				.toYRot()) + 180));
	}

	/**
	 * A block that also picks a face to sit on. The turns match what the old model generators produced:
	 * the face supplies the pitch, and the facing supplies the yaw with a half turn on top - and
	 * another half turn again when it hangs from a ceiling.
	 */
	public static <T extends Block> void horizontalFaceBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, state -> horizontalFaceVariant(modelFunc.apply(state),
			state.getValue(BlockStateProperties.ATTACH_FACE),
			state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	public static MultiVariant horizontalFaceVariant(MultiVariant variant, AttachFace face, Direction dir) {
		return rotateY(rotateX(variant, face.ordinal() * 90),
			(int) dir.toYRot() + 180 + (face == AttachFace.CEILING ? 180 : 0));
	}

	public static <T extends Block> void horizontalAxisBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, state -> {
			MultiVariant variant = modelFunc.apply(state);
			return state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == Axis.X
				? variant.with(BlockModelGenerators.Y_ROT_90)
				: variant;
		});
	}

	public static <T extends DirectionalAxisKineticBlock> void directionalAxisBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, BiFunction<BlockState, Boolean, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, state -> {
			boolean alongFirst = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
			Direction direction = state.getValue(DirectionalAxisKineticBlock.FACING);
			boolean vertical = direction.getAxis()
				.isHorizontal() && (direction.getAxis() == Axis.X) == alongFirst;
			int xRot = direction == Direction.DOWN ? 270 : direction == Direction.UP ? 90 : 0;
			int yRot = direction.getAxis()
				.isVertical() ? alongFirst ? 0 : 90 : (int) direction.toYRot();
			return rotateY(rotateX(modelFunc.apply(state, vertical), xRot), yRot);
		});
	}

	public static <T extends Block> void horizontalWheel(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, Function<BlockState, MultiVariant> modelFunc) {
		forAllStates(ctx, prov, state -> rotateY(modelFunc.apply(state)
			.with(BlockModelGenerators.X_ROT_90),
			((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING)
				.toYRot()) + 180));
	}

	public static <T extends Block> void cubeAll(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		String textureSubDir) {
		cubeAll(ctx, prov, textureSubDir, ctx.getName());
	}

	public static <T extends Block> void cubeAll(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		String textureSubDir, String name) {
		String texturePath = "block/" + textureSubDir + name;
		prov.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(ctx.get(),
			BlockModelGenerators.plainVariant(cubeAllModel(prov, ctx.getName(), prov.modLoc(texturePath)))));
	}

	public static NonNullBiConsumer<DataGenContext<Block, CartAssemblerBlock>, RegistrateBlockModelGenerator> cartAssembler() {
		return (c, p) -> forAllStates(c, p, state -> {
			int yRotation = state.getValue(CartAssemblerBlock.RAIL_SHAPE) == RailShape.EAST_WEST ? 270 : 0;
			if (state.getValue(CartAssemblerBlock.BACKWARDS))
				yRotation += 180;
			MultiVariant variant = BlockModelGenerators.plainVariant(p.modLoc("block/" + c.getName() + "/block_"
				+ state.getValue(CartAssemblerBlock.RAIL_TYPE)
					.getSerializedName()
				+ (state.getValue(CartAssemblerBlock.POWERED) ? "_powered" : "")));
			return rotateY(variant, yRotation);
		});
	}

	/**
	 * Deliberately generates nothing, as on 1.21.1: the burner's blockstate is written by hand.
	 */
	public static NonNullBiConsumer<DataGenContext<Block, BlazeBurnerBlock>, RegistrateBlockModelGenerator> blazeHeater() {
		return (c, p) -> {
		};
	}

	public static <B extends LinearChassisBlock> NonNullBiConsumer<DataGenContext<Block, B>, RegistrateBlockModelGenerator> linearChassis() {
		return (c, p) -> {
			Identifier side = p.modLoc("block/" + c.getName() + "_side");
			Identifier top = p.modLoc("block/linear_chassis_end");
			Identifier top_sticky = p.modLoc("block/linear_chassis_end_sticky");

			List<MultiVariant> models = new ArrayList<>(4);
			for (boolean isTopSticky : Iterate.trueAndFalse)
				for (boolean isBottomSticky : Iterate.trueAndFalse)
					models.add(BlockModelGenerators.plainVariant(p.getBuilder()
						.parent(p.mcLoc("block/cube_bottom_top"))
						.texture(TextureSlot.SIDE, new Material(side))
						.texture(TextureSlot.BOTTOM,
							new Material(
								isBottomSticky ? top_sticky : top))
						.texture(TextureSlot.TOP,
							new Material(
								isTopSticky ? top_sticky : top))
						.build(p.modLoc("block/" + c.getName() + (isTopSticky ? "_top" : "")
							+ (isBottomSticky ? "_bottom" : "")))));
			BiFunction<Boolean, Boolean, MultiVariant> modelFunc = (t, b) -> models.get((t ? 0 : 2) + (b ? 0 : 1));

			axisBlock(c, p, state -> modelFunc.apply(state.getValue(LinearChassisBlock.STICKY_TOP),
				state.getValue(LinearChassisBlock.STICKY_BOTTOM)));
		};
	}

	public static <B extends RadialChassisBlock> NonNullBiConsumer<DataGenContext<Block, B>, RegistrateBlockModelGenerator> radialChassis() {
		return (c, p) -> {
			String path = "block/" + c.getName();
			Identifier side = p.modLoc(path + "_side");
			Identifier side_sticky = p.modLoc(path + "_side_sticky");

			String templateModelPath = "block/radial_chassis";
			MultiVariant base = BlockModelGenerators.plainVariant(p.modLoc(templateModelPath + "/base"));
			List<MultiVariant> faces = new ArrayList<>(3);
			List<MultiVariant> stickyFaces = new ArrayList<>(3);

			for (Axis axis : Iterate.axes) {
				String suffix = "side_" + axis.getSerializedName();
				faces.add(BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc(templateModelPath + "/" + suffix))
					.texture(TextureSlot.SIDE,
						new Material(side))
					.build(p.modLoc("block/" + c.getName() + "_" + suffix))));
			}
			for (Axis axis : Iterate.axes) {
				String suffix = "side_" + axis.getSerializedName();
				stickyFaces.add(BlockModelGenerators.plainVariant(p.getBuilder()
					.parent(p.modLoc(templateModelPath + "/" + suffix))
					.texture(TextureSlot.SIDE,
						new Material(side_sticky))
					.build(p.modLoc("block/" + c.getName() + "_" + suffix + "_sticky"))));
			}

			MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());
			BlockState propertyGetter = c.get()
				.defaultBlockState()
				.setValue(RadialChassisBlock.AXIS, Axis.Y);

			for (Axis axis : Iterate.axes)
				builder.with(BlockModelGenerators.condition()
					.term(RadialChassisBlock.AXIS, axis),
					rotateY(rotateX(base, axis != Axis.Y ? 90 : 0), axis != Axis.X ? 0 : 90));

			for (Direction face : Iterate.horizontalDirections) {
				for (boolean sticky : Iterate.trueAndFalse) {
					for (Axis axis : Iterate.axes) {
						int horizontalAngle = (int) (face.toYRot());
						int index = axis.ordinal();
						int xRot = 0;
						int yRot = 0;

						if (axis == Axis.X)
							xRot = -horizontalAngle + 180;
						if (axis == Axis.Y)
							yRot = horizontalAngle;
						if (axis == Axis.Z) {
							yRot = -horizontalAngle + 270;

							// blockstates can't have zRot, so here we are
							if (face.getAxis() == Axis.Z) {
								index = 0;
								xRot = horizontalAngle + 180;
								yRot = 90;
							}
						}

						builder.with(BlockModelGenerators.condition()
							.term(RadialChassisBlock.AXIS, axis)
							.term(c.get()
								.getGlueableSide(propertyGetter, face), sticky),
							rotateY(rotateX((sticky ? stickyFaces : faces).get(index), xRot), yRot));
					}
				}
			}

			p.blockStateOutput.accept(builder);
		};
	}

	public static <P extends Block> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> naturalStoneTypeBlock(
		String type) {
		return (c, p) -> {
			List<Variant> variants = new ArrayList<>(4);
			for (int i = 0; i < 4; i++)
				variants.add(BlockModelGenerators.plainModel(cubeAllModel(p, type + "_natural_" + i,
					p.modLoc("block/palettes/stone_types/natural/" + type + "_" + i))));
			p.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(c.get(),
				BlockModelGenerators.variants(variants.toArray(Variant[]::new))));
		};
	}

	public static <P extends EncasedPipeBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> encasedPipe() {
		return (c, p) -> {
			MultiVariant open = AssetLookup.partialBaseVariant(c, p, "open");
			MultiVariant flat = AssetLookup.partialBaseVariant(c, p, "flat");
			MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());
			for (boolean flatPass : Iterate.trueAndFalse)
				for (Direction d : Iterate.directions) {
					int verticalAngle = d == Direction.UP ? 90 : d == Direction.DOWN ? -90 : 0;
					int yRot = (int) (d.toYRot() + (d.getAxis()
						.isVertical() ? 90 : 0));
					builder.with(
						BlockModelGenerators.condition()
							.term(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(d), !flatPass),
						rotateY(rotateX(flatPass ? flat : open, verticalAngle), yRot));
				}
			p.blockStateOutput.accept(builder);
		};
	}

	/**
	 * A trapdoor that keeps its facing when it swings open, the way the old orientable trapdoors did:
	 * the model turns to face the same way, and a top half flips over onto its back.
	 */
	public static <P extends TrapDoorBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> orientableTrapdoorBlock(
		P block, Identifier bottom, Identifier top, Identifier open) {
		return (c, p) -> forAllStates(c, p, state -> {
			boolean isOpen = state.getValue(TrapDoorBlock.OPEN);
			boolean isTop = state.getValue(TrapDoorBlock.HALF) == Half.TOP;
			boolean flipped = isOpen && isTop;
			MultiVariant variant = BlockModelGenerators.plainVariant(isOpen ? open : isTop ? top : bottom);
			return rotateY(rotateX(variant, flipped ? 180 : 0),
				((int) state.getValue(TrapDoorBlock.FACING)
					.toYRot()) + 180 + (flipped ? 180 : 0));
		}, TrapDoorBlock.POWERED, TrapDoorBlock.WATERLOGGED);
	}

	public static <P extends TrapDoorBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> uvLockedTrapdoorBlock(
		P block, Identifier bottom, Identifier top, Identifier open) {
		return (c, p) -> p.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
			.with(PropertyDispatch.initial(TrapDoorBlock.FACING, TrapDoorBlock.OPEN, TrapDoorBlock.HALF)
				.generate((facing, isOpen, half) -> {
					int yRot = isOpen ? ((int) facing.toYRot()) + 180 : 0;
					MultiVariant variant = BlockModelGenerators
						.plainVariant(isOpen ? open : half == Half.TOP ? top : bottom);
					if (!isOpen)
						variant = variant.with(BlockModelGenerators.UV_LOCK);
					return rotateY(variant, yRot);
				})));
	}

	public static <P extends WhistleExtenderBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> whistleExtender() {
		return (c, p) -> {
			String basePath = "block/steam_whistle/extension/";
			MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());

			for (WhistleSize size : WhistleSize.values()) {
				String basePathSize = basePath + size.getSerializedName() + "_";
				MultiVariant topRim = BlockModelGenerators.plainVariant(Create.asResource(basePathSize + "top_rim"));
				MultiVariant single = BlockModelGenerators.plainVariant(Create.asResource(basePathSize + "single"));
				MultiVariant double_ = BlockModelGenerators.plainVariant(Create.asResource(basePathSize + "double"));

				builder.with(BlockModelGenerators.condition()
					.term(WhistleExtenderBlock.SIZE, size)
					.term(WhistleExtenderBlock.SHAPE, WhistleExtenderShape.DOUBLE), topRim);
				builder.with(BlockModelGenerators.condition()
					.term(WhistleExtenderBlock.SIZE, size)
					.term(WhistleExtenderBlock.SHAPE, WhistleExtenderShape.SINGLE), single);
				builder.with(BlockModelGenerators.condition()
					.term(WhistleExtenderBlock.SIZE, size)
					.term(WhistleExtenderBlock.SHAPE, WhistleExtenderShape.DOUBLE,
						WhistleExtenderShape.DOUBLE_CONNECTED),
					double_);
			}

			p.blockStateOutput.accept(builder);
		};
	}

	public static <P extends FluidPipeBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockModelGenerator> pipe() {
		return (c, p) -> {
			String path = "block/" + c.getName();

			String LU = "lu";
			String RU = "ru";
			String LD = "ld";
			String RD = "rd";
			String LR = "lr";
			String UD = "ud";
			String U = "u";
			String D = "d";
			String L = "l";
			String R = "r";

			List<String> orientations = ImmutableList.of(LU, RU, LD, RD, LR, UD, U, D, L, R);
			Map<String, Pair<Integer, Integer>> uvs = ImmutableMap.<String, Pair<Integer, Integer>>builder()
				.put(LU, Pair.of(12, 4))
				.put(RU, Pair.of(8, 4))
				.put(LD, Pair.of(12, 0))
				.put(RD, Pair.of(8, 0))
				.put(LR, Pair.of(4, 8))
				.put(UD, Pair.of(0, 8))
				.put(U, Pair.of(4, 4))
				.put(D, Pair.of(0, 0))
				.put(L, Pair.of(4, 0))
				.put(R, Pair.of(0, 4))
				.build();

			Map<Axis, Identifier> coreTemplates = new IdentityHashMap<>();
			Map<Pair<String, Axis>, MultiVariant> coreModels = new HashMap<>();

			for (Axis axis : Iterate.axes)
				coreTemplates.put(axis, p.modLoc(path + "/core_" + axis.getSerializedName()));

			for (Axis axis : Iterate.axes) {
				Identifier parent = coreTemplates.get(axis);
				for (String s : orientations) {
					Pair<String, Axis> key = Pair.of(s, axis);
					String modelName = path + "/" + s + "_" + axis.getSerializedName();
					Pair<Integer, Integer> pair = uvs.get(s);
					float u = pair.getKey();
					float v = pair.getValue();
					coreModels.put(key, BlockModelGenerators.plainVariant(p.getBuilder()
						.parent(parent)
						.transformTemplate(t -> t.element(element -> {
							element.from(4, 4, 4)
								.to(12, 12, 12);
							element.face(Direction.get(AxisDirection.POSITIVE, axis), f -> {
							});
							element.face(Direction.get(AxisDirection.NEGATIVE, axis), f -> {
							});
							element.faces((d, face) -> {
								if (d == Direction.UP)
									face.uvs(u + 4, v + 4, u, v);
								if (d == Direction.DOWN)
									face.uvs(u + 4, v, u, v + 4);
								if (d == Direction.NORTH)
									face.uvs(u, v, u + 4, v + 4);
								if (d == Direction.SOUTH)
									face.uvs(u + 4, v, u, v + 4);
								if (d == Direction.EAST)
									face.uvs(u, v, u + 4, v + 4);
								if (d == Direction.WEST)
									face.uvs(u + 4, v, u, v + 4);
								face.texture(TextureSlot.create("0"));
							});
						}))
						.build(p.modLoc(modelName))));
				}
			}

			MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());
			for (Axis axis : Iterate.axes) {
				putPart(coreModels, builder, axis, LU, true, false, true, false);
				putPart(coreModels, builder, axis, RU, true, false, false, true);
				putPart(coreModels, builder, axis, LD, false, true, true, false);
				putPart(coreModels, builder, axis, RD, false, true, false, true);
				putPart(coreModels, builder, axis, UD, true, true, false, false);
				putPart(coreModels, builder, axis, U, true, false, false, false);
				putPart(coreModels, builder, axis, D, false, true, false, false);
				putPart(coreModels, builder, axis, LR, false, false, true, true);
				putPart(coreModels, builder, axis, L, false, false, true, false);
				putPart(coreModels, builder, axis, R, false, false, false, true);
			}
			p.blockStateOutput.accept(builder);
		};
	}

	private static void putPart(Map<Pair<String, Axis>, MultiVariant> coreModels, MultiPartGenerator builder,
		Axis axis, String s, boolean up, boolean down, boolean left, boolean right) {
		Direction positiveAxis = Direction.get(AxisDirection.POSITIVE, axis);
		Map<Direction, BooleanProperty> propertyMap = FluidPipeBlock.PROPERTY_BY_DIRECTION;

		Direction upD = Pointing.UP.getCombinedDirection(positiveAxis);
		Direction leftD = Pointing.LEFT.getCombinedDirection(positiveAxis);
		Direction rightD = Pointing.RIGHT.getCombinedDirection(positiveAxis);
		Direction downD = Pointing.DOWN.getCombinedDirection(positiveAxis);

		if (axis == Axis.Y || axis == Axis.X) {
			leftD = leftD.getOpposite();
			rightD = rightD.getOpposite();
		}

		builder.with(BlockModelGenerators.condition()
			.term(propertyMap.get(upD), up)
			.term(propertyMap.get(leftD), left)
			.term(propertyMap.get(rightD), right)
			.term(propertyMap.get(downD), down), coreModels.get(Pair.of(s, axis)));
	}

	public static Function<BlockState, MultiVariant> mapToAir(RegistrateBlockModelGenerator p) {
		return state -> BlockModelGenerators.plainVariant(p.mcLoc("block/air"));
	}

}
