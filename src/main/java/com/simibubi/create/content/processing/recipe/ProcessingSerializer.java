package com.simibubi.create.content.processing.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Pairs a processing recipe's serializer with the factory that builds it.
 * <p>
 * Minecraft 26.2 turned {@link RecipeSerializer} into a final record, so Create can no longer hang
 * the factory off its own serializer subclass - and datagen needs that factory to build recipes of
 * the right subtype. The serializer registered with the game is the record this holds; everything
 * that needs the factory asks the recipe type for this instead.
 */
public class ProcessingSerializer<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>> {

	private final ProcessingRecipe.Factory<P, R> factory;
	private final RecipeSerializer<R> recipeSerializer;

	public ProcessingSerializer(ProcessingRecipe.Factory<P, R> factory, MapCodec<P> paramsCodec,
		StreamCodec<RegistryFriendlyByteBuf, P> paramsStreamCodec) {
		this.factory = factory;
		this.recipeSerializer = new RecipeSerializer<>(ProcessingRecipe.codec(factory, paramsCodec),
			ProcessingRecipe.streamCodec(factory, paramsStreamCodec));
	}

	public ProcessingRecipe.Factory<P, R> factory() {
		return factory;
	}

	public RecipeSerializer<R> recipeSerializer() {
		return recipeSerializer;
	}

}
