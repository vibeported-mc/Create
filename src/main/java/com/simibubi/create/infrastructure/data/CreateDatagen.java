package com.simibubi.create.infrastructure.data;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.compat.curios.CuriosDataGenerator;
import com.simibubi.create.content.equipment.armor.AllEquipmentAssets;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.data.recipe.CreateMechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.CreateSequencedAssemblyRecipeGen;
import com.simibubi.create.foundation.data.recipe.CreateStandardRecipeGen;
import com.simibubi.create.foundation.data.CreateDatamapProvider;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.ponder.api.client.PonderIndex;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class CreateDatagen {
	private static boolean addedExtraRegistrateData;
	private static boolean boundComponents;
	@Nullable
	private static CompletableFuture<HolderLookup.Provider> componentRegistries;
	public static void gatherDataHighPriority() {
		if (addedExtraRegistrateData)
			return;
		addedExtraRegistrateData = true;
		addExtraRegistrateData();
	}

	/**
	 * 26.2 fires a separate event per side instead of handing one event a pair of include flags, and
	 * providers are added to the event rather than to the generator. The event is also raised per mod,
	 * so there is no mod set to filter on any more.
	 */
	public static void gatherData(GatherDataEvent.Server event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
		componentRegistries = lookupProvider;

		GeneratedEntriesProvider generatedEntriesProvider = new GeneratedEntriesProvider(output, lookupProvider);
		lookupProvider = generatedEntriesProvider.getRegistryProvider();
		event.addProvider(generatedEntriesProvider);

		CompletableFuture<HolderLookup.Provider> registries = lookupProvider;
		event.addProvider(new CreateRecipeSerializerTagsProvider(output, registries));
		event.addProvider(new CreateContraptionTypeTagsProvider(output, registries));
		event.addProvider(new CreateMountedItemStorageTypeTagsProvider(output, registries));
		event.addProvider(new DamageTypeTagGen(output, registries));
		event.addProvider(new AllAdvancements(output, registries));
		event.addProvider(BaseRecipeProvider.runner(output, registries, "Create's Standard Recipes",
			CreateStandardRecipeGen::new));
		event.addProvider(BaseRecipeProvider.runner(output, registries, "Create's Mechanical Crafting Recipes",
			CreateMechanicalCraftingRecipeGen::new));
		event.addProvider(BaseRecipeProvider.runner(output, registries, "Create's Sequenced Assembly Recipes",
			CreateSequencedAssemblyRecipeGen::new));
		event.addProvider(CreateRecipeProvider.allProcessing(output, registries));
		event.addProvider(new CreateDatamapProvider(output, registries));
		event.addProvider(new VanillaHatOffsetGenerator(output, registries));
		event.addProvider(new CuriosDataGenerator(output, registries));
		event.addProvider(new CreateEnchantmentTagsProvider(output, registries));
	}

	public static void gatherData(GatherDataEvent.Client event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		componentRegistries = event.getLookupProvider();

		event.addProvider(new AllEquipmentAssets(output));
		event.addProvider(AllSoundEvents.provider(generator));
		event.addProvider(new CreateWikiBlockInfoProvider(output));
	}

	private static void addExtraRegistrateData() {
		CreateRegistrateTags.addGenerators();

		Create.registrate().addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> langConsumer = provider::add;

			provideDefaultLang("interface", langConsumer);
			provideDefaultLang("tooltips", langConsumer);
			AllAdvancements.provideLang(langConsumer);
			AllSoundEvents.provideLang(langConsumer);
			AllKeys.provideLang(langConsumer);
			providePonderLang(langConsumer);
			new TagLangGenerator(langConsumer).generate();
		});
	}

	private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
		String path = "assets/create/lang/default/" + fileName + ".json";
		JsonElement jsonElement = FilesHelper.loadJsonResource(path);
		if (jsonElement == null) {
			throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
		}
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getAsString();
			consumer.accept(key, value);
		}
	}

	/**
	 * An item's default components are bound when a server loads its datapacks, which never happens
	 * during datagen - so an ItemStack built here fails on "Components not bound yet", and the ponder
	 * scenes are full of stacks. Bind them from the registries datagen was handed.
	 * <p>
	 * One wrinkle: those registries answer a tag lookup with an unbound placeholder, an anonymous
	 * subclass of HolderSet.Named. NeoForge only whitelists Named itself as a component value, so the
	 * placeholder trips its equals/hashCode check; say up front that the placeholder is fine too.
	 */
	private static void bindItemComponents() {
		if (boundComponents || componentRegistries == null)
			return;
		boundComponents = true;

		CommonHooks.markComponentClassAsValid(HolderSet.emptyNamed(new HolderOwner<>() {
		}, TagKey.create(Registries.ITEM, Create.asResource("datagen_placeholder")))
			.getClass());

		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(componentRegistries.join())
			.forEach(DataComponentInitializers.PendingComponents::apply);
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		bindItemComponents();

		// Register this since FMLClientSetupEvent does not run during datagen
		PonderIndex.addPlugin(new CreatePonderPlugin());

		PonderIndex.getLangAccess().provideLang(Create.ID, consumer);
	}
}
