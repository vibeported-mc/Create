package com.simibubi.create.foundation.data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.tags.TagEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagGen {
	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOrPickaxe() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_AXE)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOnly() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_AXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		CommonMetal.ItemLikeTag tag) {
		return tagBlockAndItem(Map.of(tag.blocks(), tag.items()));
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		TagKey<Block> blockTag, TagKey<Item> itemTag) {
		return tagBlockAndItem(Map.of(blockTag, itemTag));
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		Map<TagKey<Block>, TagKey<Item>> tags) {
		return b -> {
			for (TagKey<Block> blockTag : tags.keySet()) {
				b.tag(blockTag);
			}
			ItemBuilder<BlockItem, BlockBuilder<T, P>> item = b.item();
			for (TagKey<Item> itemTag : tags.values()) {
				item.tag(itemTag);
			}
			return item;
		};
	}

	public static <T> TagAppender<T> addOptional(TagAppender<T> appender, Mods mod, String id) {
		appender.add(TagEntry.optionalElement(mod.asResource(id)));
		return appender;
	}

	public static <T> TagAppender<T> addOptional(TagAppender<T> appender, Mods mod, List<String> ids) {
		for (String id : ids)
			appender.add(TagEntry.optionalElement(mod.asResource(id)));
		return appender;
	}

	public static <T> CreateTagAppender<T> addOptional(CreateTagAppender<T> appender, Mods mod, String id) {
		appender.addOptional(mod.asResource(id));
		return appender;
	}

	public static <T> CreateTagAppender<T> addOptional(CreateTagAppender<T> appender, Mods mod, List<String> ids) {
		for (String id : ids)
			appender.addOptional(mod.asResource(id));
		return appender;
	}

	public static class CreateTagsProvider<T> {
		private final RegistrateTagsProvider<T> provider;
		private final Function<T, ResourceKey<T>> keyExtractor;

		public CreateTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Holder.Reference<T>> refExtractor) {
			this.provider = provider;
			this.keyExtractor = refExtractor.andThen(Holder.Reference::key);
		}

		public CreateTagAppender<T> tag(TagKey<T> tag) {
			// Registrate's provider interface exposes the raw builder rather than an appender.
			TagBuilder builder = getOrCreateRawBuilder(tag);
			return new CreateTagAppender<>(TagAppender.forBuilder(builder), keyExtractor, builder);
		}

		public TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
			return provider.rawBuilder(tag);
		}
	}

	public static class CreateTagAppender<T> {

		private final TagAppender<T> delegate;
		private final Function<T, ResourceKey<T>> keyExtractor;

		private final TagBuilder builder;

		public CreateTagAppender(TagAppender<T> delegate, Function<T, ResourceKey<T>> keyExtractor,
			TagBuilder builder) {
			this.delegate = delegate;
			this.keyExtractor = keyExtractor;
			this.builder = builder;
		}

		public TagAppender<T> delegate() {
			return delegate;
		}

		public CreateTagAppender<T> add(ResourceKey<T> key) {
			delegate.add(key);
			return this;
		}

		public CreateTagAppender<T> add(T entry) {
			delegate.add(this.keyExtractor.apply(entry));
			return this;
		}

		@SafeVarargs
		public final CreateTagAppender<T> add(T... entries) {
			Stream.<T>of(entries)
				.map(this.keyExtractor)
				.forEach(delegate::add);
			return this;
		}

		public CreateTagAppender<T> addOptional(Identifier id) {
			delegate.add(TagEntry.optionalElement(id));
			return this;
		}

		public CreateTagAppender<T> addTag(TagKey<T> tag) {
			delegate.addTag(tag);
			return this;
		}

		public CreateTagAppender<T> addOptionalTag(TagKey<T> tag) {
			delegate.addOptionalTag(tag);
			return this;
		}

		/**
		 * 26.2 dropped removal from the tag appender; it lives on the builder now.
		 */
		public CreateTagAppender<T> remove(Identifier... ids) {
			for (Identifier id : ids)
				builder.remove(TagEntry.element(id));
			return this;
		}

	}
}
