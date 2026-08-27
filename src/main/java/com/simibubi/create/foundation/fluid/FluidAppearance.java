package com.simibubi.create.foundation.fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * How a fluid looks, for Create's own fluid rendering.
 * <p>
 * Minecraft 26.2 moved a fluid's textures and tint out of NeoForge's client extensions and into a
 * baked {@link FluidModel} held by the model manager, keyed on the fluid state. Create renders fluid
 * from a {@link FluidStack} in a dozen places, so the lookup lives here rather than being repeated.
 */
@OnlyIn(Dist.CLIENT)
public class FluidAppearance {

	public static FluidModel modelOf(FluidStack fluidStack) {
		return modelOf(fluidStack.getFluid()
			.defaultFluidState());
	}

	public static FluidModel modelOf(FluidState state) {
		return Minecraft.getInstance()
			.getModelManager()
			.getFluidStateModelSet()
			.get(state);
	}

	public static TextureAtlasSprite stillTexture(FluidStack fluidStack) {
		return modelOf(fluidStack).stillMaterial()
			.sprite();
	}

	public static TextureAtlasSprite flowingTexture(FluidStack fluidStack) {
		return modelOf(fluidStack).flowingMaterial()
			.sprite();
	}

	/**
	 * The fluid's tint as an opaque ARGB colour. Fluids without a tint source render untinted.
	 */
	public static int tintColor(FluidStack fluidStack) {
		FluidTintSource tint = modelOf(fluidStack).fluidTintSource();
		return tint == null ? 0xFF_FFFFFF : tint.colorAsStack(fluidStack);
	}

	private FluidAppearance() {
	}

}
