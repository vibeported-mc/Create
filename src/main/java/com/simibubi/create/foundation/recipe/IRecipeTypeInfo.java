package com.simibubi.create.foundation.recipe;

import org.jetbrains.annotations.Nullable;
import com.simibubi.create.content.processing.recipe.ProcessingSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public interface IRecipeTypeInfo {

	Identifier getId();

	<T extends RecipeSerializer<?>> T getSerializer();

	<I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType();

	/**
	 * The processing serializer this type was built from, or null when the type is not a processing
	 * recipe. 26.2's {@link RecipeSerializer} is a final record, so the factory datagen needs can no
	 * longer live on the serializer itself.
	 */
	@Nullable
	default ProcessingSerializer<?, ?> getProcessingSerializer() {
		return null;
	}

}
