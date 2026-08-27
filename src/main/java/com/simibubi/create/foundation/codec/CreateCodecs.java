package com.simibubi.create.foundation.codec;

import com.simibubi.create.foundation.item.ItemStackHandler;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.item.ItemSlots;

import net.minecraft.util.ExtraCodecs;

import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CreateCodecs {
	public static final Codec<Integer> INT_STR = Codec.STRING.comapFlatMap(
		string -> {
			try {
				return DataResult.success(Integer.parseInt(string));
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not an integer: " + string);
			}
		},
		String::valueOf
	);

	public static final Codec<ItemStackHandler> ITEM_STACK_HANDLER = Codec.lazyInitialized(() -> ItemSlots.CODEC.xmap(
		slots -> slots.toHandler(ItemStackHandler::new), ItemSlots::fromHandler
	));

	public static Codec<Integer> boundedIntStr(int min) {
		return INT_STR.validate(i -> i >= min ? DataResult.success(i) : DataResult.error(() -> "Value under minimum of " + min));
	}

	public static final Codec<Double> NON_NEGATIVE_DOUBLE = doubleRangeWithMessage(0, Double.MAX_VALUE,
		i -> "Value must be non-negative: " + i);
	public static final Codec<Double> POSITIVE_DOUBLE = doubleRangeWithMessage(1, Double.MAX_VALUE,
		i -> "Value must be positive: " + i);

	private static Codec<Double> doubleRangeWithMessage(double min, double max, Function<Double, String> errorMessage) {
		return Codec.DOUBLE.validate(i ->
			i.compareTo(min) >= 0 && i.compareTo(max) <= 0 ? DataResult.success(i) : DataResult.error(() ->
				errorMessage.apply(i)
			)
		);
	}

	/**
	 * A fluid ingredient written flat: its type beside its own fields, rather than nested under a key.
	 * <p>
	 * 26.2 dropped {@code FluidIngredient.MAP_CODEC_NONEMPTY}; dispatching on the type registry gives
	 * back the same shape, since a dispatch map codec inlines the dispatched fields.
	 */
	private static final MapCodec<FluidIngredient> FLAT_FLUID_INGREDIENT = NeoForgeRegistries.FLUID_INGREDIENT_TYPES
		.byNameCodec()
		.dispatchMap("type", FluidIngredient::getType, FluidIngredientType::codec);

	public static Codec<SizedFluidIngredient> FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE = RecordCodecBuilder.create(instance -> instance.group(
		FLAT_FLUID_INGREDIENT.forGetter(SizedFluidIngredient::ingredient),
		NeoForgeExtraCodecs.optionalFieldAlwaysWrite(ExtraCodecs.POSITIVE_INT, "amount", 1000).forGetter(SizedFluidIngredient::amount)
	).apply(instance, SizedFluidIngredient::new));

	/**
	 * The pre-1.21.1 {@code fluid_stack}/{@code fluid_tag} spellings were scheduled for removal in this
	 * port, and their fluid ingredient types no longer exist in 26.2, so only the flat form is read.
	 */
	public static Codec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT = FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE;
}
