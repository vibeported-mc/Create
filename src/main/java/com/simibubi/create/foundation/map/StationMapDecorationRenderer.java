package com.simibubi.create.foundation.map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.map.IMapDecorationRenderer;

public class StationMapDecorationRenderer implements IMapDecorationRenderer {

	@Override
	public boolean render(MapRenderState.MapDecorationRenderState decoration, PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector, MapRenderState mapRenderState, TextureAtlas decorationSprites,
		boolean inItemFrame, int packedLight, int index) {
		poseStack.pushPose();

		poseStack.translate(decoration.x / 2.0F + 64.0F, decoration.y / 2.0F + 64.0F, -0.02F);

		poseStack.pushPose();

		poseStack.translate(0.5f, 0f, 0);
		poseStack.scale(4.5F, 4.5F, 3.0F);

		TextureAtlasSprite sprite = decoration.atlasSprite;
		if (sprite != null) {
			float u0 = sprite.getU0();
			float v0 = sprite.getV0();
			float u1 = sprite.getU1();
			float v1 = sprite.getV1();
			float z = index * -0.001f;
			submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(sprite.atlasLocation()),
				(pose, buffer) -> {
					buffer.addVertex(pose, -1.0F, 1.0F, z)
						.setColor(-1)
						.setUv(u0, v0)
						.setLight(packedLight);
					buffer.addVertex(pose, 1.0F, 1.0F, z)
						.setColor(-1)
						.setUv(u1, v0)
						.setLight(packedLight);
					buffer.addVertex(pose, 1.0F, -1.0F, z)
						.setColor(-1)
						.setUv(u1, v1)
						.setLight(packedLight);
					buffer.addVertex(pose, -1.0F, -1.0F, z)
						.setColor(-1)
						.setUv(u0, v1)
						.setLight(packedLight);
				});
		}

		poseStack.popPose();

		Component name = decoration.name;
		if (name != null) {
			Font font = Minecraft.getInstance().font;
			float width = font.width(name);
			poseStack.pushPose();
			poseStack.translate(0, 6.0F, -0.005F);

			poseStack.scale(0.8f, 0.8f, 1.0F);
			poseStack.translate(-width / 2f + .5f, 0, 0);
			submitNodeCollector.order(1)
				.submitText(poseStack, 0.0F, 0.0F, name.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
					packedLight, -1, Integer.MIN_VALUE, 0);
			poseStack.popPose();
		}

		poseStack.popPose();

		return true;
	}
}
