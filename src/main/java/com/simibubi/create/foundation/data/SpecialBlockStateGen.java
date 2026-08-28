package com.simibubi.create.foundation.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class SpecialBlockStateGen {

	protected Property<?>[] getIgnoredProperties() {
		return new Property<?>[0];
	}

	/**
	 * 26.2 dispatches a blockstate over named properties rather than mapping a function across every
	 * state, and the typed {@code PropertyDispatch.initial} overloads want those properties known when
	 * the code is written. Which properties matter here is decided by the subclass at runtime, so the
	 * dispatch is assembled by hand: every state that survives the ignored list contributes one entry,
	 * keyed by the values of the properties that were kept.
	 */
	public final <T extends Block> void generate(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov) {
		T block = ctx.getEntry();
		List<Property<?>> ignored = Arrays.asList(getIgnoredProperties());
		List<Property<?>> dispatched = new ArrayList<>(block.getStateDefinition()
			.getProperties());
		dispatched.removeAll(ignored);

		class DynamicDispatch extends PropertyDispatch<MultiVariant> {
			@Override
			public List<Property<?>> getDefinedProperties() {
				return dispatched;
			}

			void put(PropertyValueList key, MultiVariant variant) {
				putValue(key, variant);
			}
		}

		DynamicDispatch dispatch = new DynamicDispatch();

		List<PropertyValueList> seen = new ArrayList<>();
		for (BlockState state : block.getStateDefinition()
			.getPossibleStates()) {
			PropertyValueList key = PropertyValueList.EMPTY;
			for (Property<?> property : dispatched)
				key = key.extend(valueOf(state, property));
			if (seen.contains(key))
				continue;
			seen.add(key);

			MultiVariant variant = getModel(ctx, prov, state);
			variant = rotate(variant, (getXRotation(state) + 360) % 360, true);
			variant = rotate(variant, (getYRotation(state) + 360) % 360, false);
			dispatch.put(key, variant);
		}

		prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
			.with(dispatch));
	}

	private static <V extends Comparable<V>> Property.Value<V> valueOf(BlockState state, Property<V> property) {
		return property.value(state.getValue(property));
	}

	private static MultiVariant rotate(MultiVariant variant, int degrees, boolean x) {
		return switch (degrees) {
			case 90 -> variant.with(x ? BlockModelGenerators.X_ROT_90 : BlockModelGenerators.Y_ROT_90);
			case 180 -> variant.with(x ? BlockModelGenerators.X_ROT_180 : BlockModelGenerators.Y_ROT_180);
			case 270 -> variant.with(x ? BlockModelGenerators.X_ROT_270 : BlockModelGenerators.Y_ROT_270);
			default -> variant;
		};
	}

	protected int horizontalAngle(Direction direction) {
		if (direction.getAxis()
			.isVertical())
			return 0;
		return (int) direction.toYRot();
	}

	protected abstract int getXRotation(BlockState state);

	protected abstract int getYRotation(BlockState state);

	public abstract <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov, BlockState state);

}
