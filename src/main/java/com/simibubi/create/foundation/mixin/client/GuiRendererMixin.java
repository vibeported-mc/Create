package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.infrastructure.gui.CreatePanorama;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;

/**
 * Lets Create's main menu show its own panorama.
 * <p>
 * The GUI renderer owns the one cube map the panorama is drawn from, so the only way to show another is
 * to swap it out for the frame Create's menu asked for.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/CubeMap;render(FF)V"))
	private void create$swapPanorama(CubeMap cubeMap, float rotX, float rotY) {
		CreatePanorama.pick(cubeMap)
			.render(rotX, rotY);
	}

}
