package com.simibubi.create.foundation.advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class CreateAdvancement {

	static final Identifier BACKGROUND = Create.asResource("textures/gui/advancements.png");
	static final String LANG = "advancement." + Create.ID + ".";
	static final String SECRET_SUFFIX = "\n\u00A77(Hidden Advancement)";

	private final Advancement.Builder mcBuilder = Advancement.Builder.advancement();
	private SimpleCreateTrigger builtinTrigger;
	private CreateAdvancement parent;
	private final Builder createBuilder = new Builder();

	AdvancementHolder datagenResult;

	private String id;
	private String title;
	private String description;

	public CreateAdvancement(String id, UnaryOperator<Builder> b) {
		this.id = id;

		b.apply(createBuilder);

		if (!createBuilder.externalTrigger) {
			builtinTrigger = AllTriggers.addSimple(id + "_builtin");
			mcBuilder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
		}

		if (createBuilder.type == TaskType.SECRET)
			description += SECRET_SUFFIX;

		AllAdvancements.ENTRIES.add(this);
	}

	private String titleKey() {
		return LANG + id;
	}

	private String descriptionKey() {
		return titleKey() + ".desc";
	}

	public boolean isAlreadyAwardedTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return true;
		AdvancementHolder advancement = sp.level()
			.getServer()
			.getAdvancements()
			.get(Create.asResource(id));
		if (advancement == null)
			return true;
		return sp.getAdvancements()
			.getOrStartProgress(advancement)
			.isDone();
	}

	public void awardTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return;
		if (builtinTrigger == null)
			throw new UnsupportedOperationException(
				"Advancement " + id + " uses external Triggers, it cannot be awarded directly");
		builtinTrigger.trigger(sp);
	}

	/**
	 * The lookup datagen is running with, for triggers that name a tag. 26.2 resolves a tag to a
	 * holder set when the predicate is built, and the built-in registries carry no tags at that point;
	 * the datagen provider does.
	 */
	static HolderLookup.Provider datagenRegistries;

	void save(Consumer<AdvancementHolder> t, HolderLookup.Provider registries) {
		datagenRegistries = registries;
		if (parent != null)
			mcBuilder.parent(parent.datagenResult);

		if (createBuilder.func != null) {
			ItemStackTemplate built = createBuilder.func.apply(registries);
			createBuilder.icon = () -> built;
			createBuilder.iconItem = () -> built.item()
				.value();
		}

		for (int i = 0; i < createBuilder.triggers.size(); i++)
			mcBuilder.addCriterion(String.valueOf(i), createBuilder.triggers.get(i)
				.get());

		mcBuilder.display(createBuilder.icon.get(), Component.translatable(titleKey()),
			Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)),
			id.equals("root") ? BACKGROUND : null, createBuilder.type.advancementType, createBuilder.type.toast,
			createBuilder.type.announce, createBuilder.type.hide);

		datagenResult = mcBuilder.save(t, Create.asResource(id)
			.toString());
	}

	void provideLang(BiConsumer<String, String> consumer) {
		consumer.accept(titleKey(), title);
		consumer.accept(descriptionKey(), description);
	}

	static enum TaskType {

		SILENT(AdvancementType.TASK, false, false, false),
		NORMAL(AdvancementType.TASK, true, false, false),
		NOISY(AdvancementType.TASK, true, true, false),
		EXPERT(AdvancementType.GOAL, true, true, false),
		SECRET(AdvancementType.GOAL, true, true, true),

		;

		private final AdvancementType advancementType;
		private final boolean toast;
		private final boolean announce;
		private final boolean hide;

		TaskType(AdvancementType advancementType, boolean toast, boolean announce, boolean hide) {
			this.advancementType = advancementType;
			this.toast = toast;
			this.announce = announce;
			this.hide = hide;
		}
	}

	class Builder {

		private TaskType type = TaskType.NORMAL;
		private boolean externalTrigger;
		// 26.2 binds item components and tags only after registration, and these advancements are
		// declared while the registry events are still running, so a criterion cannot name an item or
		// a tag until the advancement is actually saved.
		private final List<Supplier<Criterion<?>>> triggers = new ArrayList<>();
		// 26.2 binds an item's default components only once the item registry freezes, which is after
		// the registry events this class is built from. An ItemStackTemplate names an item without
		// touching its components, and the icon is resolved when the advancement is saved.
		private Supplier<ItemStackTemplate> icon;
		private Supplier<Item> iconItem;
		private Function<Provider, ItemStackTemplate> func;

		Builder special(TaskType type) {
			this.type = type;
			return this;
		}

		Builder after(CreateAdvancement other) {
			CreateAdvancement.this.parent = other;
			return this;
		}

		Builder icon(ItemProviderEntry<?, ?> item) {
			icon = item::asStackTemplate;
			iconItem = item::asItem;
			return this;
		}

		Builder icon(ItemLike item) {
			icon = () -> new ItemStackTemplate(item.asItem());
			iconItem = item::asItem;
			return this;
		}

		/**
		 * Named by item rather than copied from the stack: an advancement icon only ever carries the
		 * item, and reading a stack's components during datagen comes too early for them to be bound.
		 */
		Builder icon(ItemStack stack) {
			icon = () -> new ItemStackTemplate(stack.getItem());
			iconItem = stack::getItem;
			return this;
		}

		Builder icon(Supplier<ItemStack> stack) {
			icon = () -> new ItemStackTemplate(stack.get()
				.getItem());
			iconItem = () -> stack.get()
				.getItem();
			return this;
		}

		Builder icon(Function<Provider, ItemStackTemplate> func) {
			this.func = func;
			return this;
		}

		Builder title(String title) {
			CreateAdvancement.this.title = title;
			return this;
		}

		Builder description(String description) {
			CreateAdvancement.this.description = description;
			return this;
		}

		Builder whenBlockPlaced(Block block) {
			return externalTrigger(() -> ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
		}

		Builder whenIconCollected() {
			return externalTrigger(() -> InventoryChangeTrigger.TriggerInstance.hasItems(iconItem.get()));
		}

		Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
			return externalTrigger(() -> InventoryChangeTrigger.TriggerInstance.hasItems(item.asItem()));
		}

		Builder whenItemCollected(ItemLike itemProvider) {
			return externalTrigger(() -> InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
		}

		Builder whenItemCollected(TagKey<Item> tag) {
			return externalTrigger(() -> InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item()
				.of(datagenRegistries.lookupOrThrow(Registries.ITEM), tag)
				.build()));
		}

		Builder awardedForFree() {
			return externalTrigger(() -> InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[] {}));
		}

		Builder externalTrigger(Criterion<?> trigger) {
			return externalTrigger(() -> trigger);
		}

		Builder externalTrigger(Supplier<Criterion<?>> trigger) {
			triggers.add(trigger);
			externalTrigger = true;
			return this;
		}

	}

}
