package com.simibubi.create.infrastructure.data;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllTags.AllRecipeSerializerTags;
import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.crafting.RecipeSerializer;


public class CreateRecipeSerializerTagsProvider extends TagsProvider<RecipeSerializer<?>> {
	public CreateRecipeSerializerTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, Registries.RECIPE_SERIALIZER, lookupProvider, Create.ID);
	}

	@Override
	protected void addTags(Provider pProvider) {
		tag(AllRecipeSerializerTags.AUTOMATION_IGNORE.tag).addOptional(Mods.OCCULTISM.rl("spirit_trade"))
		.addOptional(Mods.OCCULTISM.rl("ritual"));
	}

	@Override
	public String getName() {
		return "Create's Recipe Serializer Tags";
	}
}
