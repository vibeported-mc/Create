package com.simibubi.create.content.kinetics.crafter;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe on the mechanical crafter's own grid.
 * <p>
 * This used to extend {@link net.minecraft.world.item.crafting.ShapedRecipe}, but 26.2 pins that
 * class's serializer type to itself, so the shaped behaviour Create actually uses - pattern matching
 * over a grid, with an option to refuse mirrored placements - is spelled out here on top of
 * {@link NormalCraftingRecipe} instead.
 */
public class MechanicalCraftingRecipe extends NormalCraftingRecipe {

	public static final MapCodec<MechanicalCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
		CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
		ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
		ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
		Codec.BOOL.fieldOf("accept_mirrored").forGetter(MechanicalCraftingRecipe::acceptsMirrored)
	).apply(instance, MechanicalCraftingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe> STREAM_CODEC =
		StreamCodec.composite(
			Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
			CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
			ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
			ItemStackTemplate.STREAM_CODEC, r -> r.result,
			ByteBufCodecs.BOOL, MechanicalCraftingRecipe::acceptsMirrored,
			MechanicalCraftingRecipe::new);

	public static final RecipeSerializer<MechanicalCraftingRecipe> SERIALIZER =
		new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private final ShapedRecipePattern pattern;
	private final ItemStackTemplate result;
	private final boolean acceptMirrored;

	public MechanicalCraftingRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
									ShapedRecipePattern pattern, ItemStackTemplate result, boolean acceptMirrored) {
		super(commonInfo, bookInfo);
		this.pattern = pattern;
		this.result = result;
		this.acceptMirrored = acceptMirrored;
	}

	@Override
	public boolean matches(CraftingInput input, Level worldIn) {
		if (!(input instanceof MechanicalCraftingInput))
			return false;
		if (acceptsMirrored())
			return pattern.matches(input);

		// From ShapedRecipe except the symmetry
		for (int i = 0; i <= input.width() - this.getWidth(); ++i)
			for (int j = 0; j <= input.height() - this.getHeight(); ++j)
				if (this.matchesSpecific(input, i, j))
					return true;
		return false;
	}

	// From ShapedRecipe
	private boolean matchesSpecific(CraftingInput input, int p_77573_2_, int p_77573_3_) {
		List<Optional<Ingredient>> ingredients = getIngredients();
		int width = getWidth();
		int height = getHeight();
		for (int i = 0; i < input.width(); ++i) {
			for (int j = 0; j < input.height(); ++j) {
				int k = i - p_77573_2_;
				int l = j - p_77573_3_;
				Optional<Ingredient> ingredient = Optional.empty();
				if (k >= 0 && l >= 0 && k < width && l < height)
					ingredient = ingredients.get(k + l * width);
				ItemStack inSlot = input.getItem(i + j * input.width());
				if (ingredient.isEmpty() ? !inSlot.isEmpty() : !ingredient.get()
					.test(inSlot))
					return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return result.create();
	}

	/**
	 * What the recipe makes, without a crafting input to make it from.
	 *
	 * <p>The recipe has no vanilla display to read this from - it keeps itself out of the recipe
	 * book, which only knows 3x3 grids - so anything showing it, JEI above all, asks here.
	 */
	public ItemStack getResultItem() {
		return result.create();
	}

	/** The grid, one entry per cell and empty where the pattern has a gap. */
	public List<Optional<Ingredient>> getIngredients() {
		return pattern.ingredients();
	}

	public int getWidth() {
		return pattern.width();
	}

	public int getHeight() {
		return pattern.height();
	}

	@Override
	protected PlacementInfo createPlacementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeType<CraftingRecipe> getType() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getType();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public @NotNull RecipeSerializer<MechanicalCraftingRecipe> getSerializer() {
		return SERIALIZER;
	}

	public boolean acceptsMirrored() {
		return acceptMirrored;
	}

}
