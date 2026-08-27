package com.simibubi.create.foundation.advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.simibubi.create.Create;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A criterion whose condition is checked against values Create supplies at the moment it fires.
 * <p>
 * Minecraft 26.2 stopped having triggers keep their own listeners: a player's advancements hold the
 * instances for each trigger, and the trigger walks them when it fires. This does the same as
 * {@link SimpleCriterionTrigger}, testing Create's suppliers rather than a loot context.
 */
@NullMarked
public abstract class CriterionTriggerBase<T extends CriterionTriggerBase.Instance> implements CriterionTrigger<T> {

	private final Identifier id;

	public CriterionTriggerBase(String id) {
		this.id = Create.asResource(id);
	}

	public Identifier getId() {
		return id;
	}

	protected void trigger(ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
		PlayerAdvancements advancements = player.getAdvancements();
		Map<PlayerAdvancements.TriggerInstanceKey, T> listeners = advancements.getTriggerMapForType(this);
		if (listeners == null || listeners.isEmpty())
			return;

		LootContext playerContext = EntityPredicate.createContext(player, player);
		List<PlayerAdvancements.TriggerInstanceKey> matched = null;

		for (Map.Entry<PlayerAdvancements.TriggerInstanceKey, T> entry : listeners.entrySet()) {
			T instance = entry.getValue();
			if (!instance.test(suppliers))
				continue;

			Optional<ContextAwarePredicate> predicate = instance.player();
			if (predicate.isPresent() && !predicate.get()
				.matches(playerContext))
				continue;

			if (matched == null)
				matched = new ArrayList<>();
			matched.add(entry.getKey());
		}

		if (matched == null)
			return;
		for (PlayerAdvancements.TriggerInstanceKey key : matched)
			advancements.award(key.advancement(), key.criterion());
	}

	public abstract static class Instance implements SimpleCriterionTrigger.SimpleInstance {
		protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
	}

}
