package com.simibubi.create.foundation.gui.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.gui.render.pip.SmoothPipBlit;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

public class GuiCustomGeometryRenderer extends PictureInPictureRenderer<GuiCustomGeometryRenderState> {

	@Override
	public Class<GuiCustomGeometryRenderState> getRenderStateClass() {
		return GuiCustomGeometryRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiCustomGeometryRenderState renderState, PoseStack poseStack,
		SubmitNodeCollector collector) {
		if (renderState.sceneSpace()) {
			renderState.transform()
				.applyAnchorOnly(poseStack, renderState.y0(), renderState.scale());
		} else {
			renderState.transform()
				.apply(poseStack, renderState.y0(), renderState.scale());
		}
		renderState.geometry()
			.submit(poseStack, collector);
	}

	/**
	 * Linear rather than the nearest-neighbour filter vanilla blits with, so a machine's diagonals are
	 * averaged down from the supersampled texture instead of staircasing.
	 */
	@Override
	protected void blitTexture(GuiCustomGeometryRenderState renderState, GuiRenderState guiRenderState) {
		SmoothPipBlit.blit(this, renderState, guiRenderState);
	}

	@Override
	protected String getTextureLabel() {
		return "create:gui_custom_geometry";
	}

	public static void register(RegisterPictureInPictureRenderersEvent event) {
		event.register(GuiCustomGeometryRenderState.class, GuiCustomGeometryRenderer::new);
	}

}
