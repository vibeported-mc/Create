package com.simibubi.create.content.schematics.client;

import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SchematicHotbarSlotOverlay  {

	public void renderOn(GuiGraphicsExtractor graphics, int slot) {
		Window mainWindow = Minecraft.getInstance().getWindow();
		int x = mainWindow.getGuiScaledWidth() / 2 - 88;
		int y = mainWindow.getGuiScaledHeight() - 19;
		Matrix3x2fStack ms = graphics.pose();
		ms.pushMatrix();
		ms.translate((float) (0), (float) (0));
		AllGuiTextures.SCHEMATIC_SLOT.render(graphics, x + 20 * slot, y);
		ms.popMatrix();
	}

}
