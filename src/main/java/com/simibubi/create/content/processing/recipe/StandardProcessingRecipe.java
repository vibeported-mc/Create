package com.simibubi.create.content.processing.recipe;

import org.jspecify.annotations.NullMarked;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeInput;

@NullMarked
public abstract class StandardProcessingRecipe<T extends RecipeInput> extends ProcessingRecipe<T, ProcessingRecipeParams> {
	public StandardProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		super(typeInfo, params);
	}

	@FunctionalInterface
	public interface Factory<R extends StandardProcessingRecipe<?>> extends ProcessingRecipe.Factory<ProcessingRecipeParams, R> {
		R create(ProcessingRecipeParams params);
	}

	public static class Builder<R extends StandardProcessingRecipe<?>>
		extends ProcessingRecipeBuilder<ProcessingRecipeParams, R, Builder<R>> {

		public Builder(ProcessingRecipe.Factory<ProcessingRecipeParams, R> factory, Identifier recipeId) {
			super(factory, recipeId);
		}

		@Override
		protected ProcessingRecipeParams createParams() {
			return new ProcessingRecipeParams();
		}

		@Override
		public Builder<R> self() {
			return this;
		}
	}

	public static class Serializer<R extends StandardProcessingRecipe<?>>
		extends ProcessingSerializer<ProcessingRecipeParams, R> {

		public Serializer(Factory<R> factory) {
			super(factory, ProcessingRecipeParams.CODEC, ProcessingRecipeParams.STREAM_CODEC);
		}
	}
}
