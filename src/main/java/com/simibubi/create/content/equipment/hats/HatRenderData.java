package com.simibubi.create.content.equipment.hats;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfoReloadListener;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import org.jspecify.annotations.Nullable;

/**
 * The hat an entity is wearing, captured while its render state is built.
 * <p>
 * A render layer only sees the entity's render state in 26.2, and which hat an entity wears depends
 * on the entity - its head slot, its type, whether it is a train conductor. NeoForge lets a mod add
 * to any renderer's state, which is where this is worked out.
 */
public record HatRenderData(PartialModel hat, TrainHatInfo info) {

	public static final ContextKey<HatRenderData> KEY = new ContextKey<>(Create.asResource("hat"));

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void registerModifier(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier(
			(Class<? extends EntityRenderer<? extends LivingEntity, ? extends LivingEntityRenderState>>) (Class) LivingEntityRenderer.class,
			(LivingEntity entity, LivingEntityRenderState state) -> {
				PartialModel hat = EntityHats.getHatFor(entity);
				state.setRenderData(KEY,
					hat == null ? null : new HatRenderData(hat, TrainHatInfoReloadListener.getHatInfoFor(entity)));
			});
	}

	public static @Nullable HatRenderData of(EntityRenderState state) {
		return state.getRenderData(KEY);
	}

}
