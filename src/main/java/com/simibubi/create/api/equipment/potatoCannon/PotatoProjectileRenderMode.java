package com.simibubi.create.api.equipment.potatoCannon;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;

import net.minecraft.world.phys.Vec3;

// TODO: 1.21.1+ - Move into api package
public interface PotatoProjectileRenderMode {
	Codec<PotatoProjectileRenderMode> CODEC = CreateBuiltInRegistries.POTATO_PROJECTILE_RENDER_MODE.byNameCodec()
		.dispatch(PotatoProjectileRenderMode::codec, Function.identity());

	/**
	 * Everything a render mode reads off the projectile.
	 * <p>
	 * Minecraft 26.2 splits rendering into an extract phase, which may read the entity, and a submit
	 * phase, which may not. Modes only apply transforms, so they run during submission and are handed
	 * this instead of the entity.
	 *
	 * @param toCamera       from the projectile's centre towards the camera
	 * @param deltaMovement  the projectile's velocity
	 * @param age            ticks alive, including the partial tick
	 * @param randomSeed     stable per projectile, for the modes that wobble
	 */
	record Context(Vec3 toCamera, Vec3 deltaMovement, float age, int randomSeed) {
		public int random(int maxValue) {
			return (randomSeed * 31) % maxValue;
		}
	}

	void transform(PoseStack ms, Context context);

	MapCodec<? extends PotatoProjectileRenderMode> codec();
}
