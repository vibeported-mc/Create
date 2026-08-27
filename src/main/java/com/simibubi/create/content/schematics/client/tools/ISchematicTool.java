package com.simibubi.create.content.schematics.client.tools;

import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public interface ISchematicTool {

	public void init();
	public void updateSelection();

	public boolean handleRightClick();
	public boolean handleMouseWheel(double delta);

	public void submitTool(PoseStack ms, SubmitNodeCollector queue, Vec3 camera);
	public void renderOverlay(Gui gui, GuiGraphicsExtractor graphics, float partialTicks, int width, int height);
	public void submitOnSchematic(PoseStack ms, SubmitNodeCollector queue);

}
