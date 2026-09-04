package com.simibubi.create;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.simibubi.create.AllFluids.TintedFluidType;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Everything about {@link AllFluids} that only a client has: the models the fluids are drawn from,
 * and the fog they put the camera in.
 * <p>
 * These lived on {@code AllFluids} itself, which is loaded on both sides. Their bodies name
 * {@link FluidTintSource} and {@link IClientFluidTypeExtensions}, and the JVM resolves those when it
 * verifies {@code AllFluids} -- so a dedicated server could not load the class that registers
 * Create's fluids. Called from here through a static, the client types are resolved when the call
 * runs, and only a client ever runs it.
 */
public class AllFluidsClient {

	/**
	 * The model a Create fluid is drawn from.
	 * <p>
	 * Registrate already registers a model for every fluid it builds, and 26.2 rejects a second
	 * registration for the same fluid, so the tint 26.2 moved out of the fluid type travels with that
	 * model rather than through a registration of Create's own. The textures follow the naming the
	 * fluid builders use.
	 */
	public static Supplier<FluidModel.Unbaked> tintedModel(String name, Supplier<? extends Fluid> fluid) {
		return () -> {
			FluidTintSource tint = null;
			if (fluid.get()
				.getFluidType() instanceof TintedFluidType tinted)
				tint = new FluidTintSource() {
					@Override
					public int color(FluidState state) {
						return tinted.getTintColor(new FluidStack(state.getType(), 1));
					}

					@Override
					public int colorAsStack(FluidStack stack) {
						return tinted.getTintColor(stack);
					}
				};
			return new FluidModel.Unbaked(new Material(Create.asResource("fluid/" + name + "_still")),
				new Material(Create.asResource("fluid/" + name + "_flow")), null, tint);
		};
	}

	/**
	 * The fluid's own textures and tint live in its baked model in 26.2; only the fog is still a
	 * client extension.
	 */
	public static IClientFluidTypeExtensions fluidExtensions(TintedFluidType type) {
		return new IClientFluidTypeExtensions() {

			@Override
			public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
				float darkenWorldAmount, Vector4f fluidFogColor) {
				Vector3f customFogColor = type.getCustomFogColor();
				if (customFogColor != null)
					fluidFogColor.set(customFogColor.x, customFogColor.y, customFogColor.z, fluidFogColor.w);
			}

			@Override
			public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance,
				float partialTick, FogData fogData) {
				float modifier = type.getFogDistanceModifier();
				float baseWaterFog = 96.0f;
				if (modifier != 1f) {
					fogData.environmentalStart = -8;
					fogData.environmentalEnd = baseWaterFog * modifier;
				}
			}

		};
	}

	private AllFluidsClient() {}
}
