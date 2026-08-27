package com.simibubi.create.foundation.gui.render;

import com.mojang.blaze3d.vertex.PoseStack;

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
		renderState.transform()
			.apply(poseStack);
		renderState.geometry()
			.submit(poseStack, collector);
	}

	@Override
	protected String getTextureLabel() {
		return "create:gui_custom_geometry";
	}

	public static void register(RegisterPictureInPictureRenderersEvent event) {
		event.register(GuiCustomGeometryRenderState.class, GuiCustomGeometryRenderer::new);
	}

}
