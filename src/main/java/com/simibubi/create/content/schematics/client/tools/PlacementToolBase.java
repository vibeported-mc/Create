package com.simibubi.create.content.schematics.client.tools;

import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public abstract class PlacementToolBase extends SchematicToolBase {

	@Override
	public void init() {
		super.init();
	}

	@Override
	public void updateSelection() {
		super.updateSelection();
	}

	@Override
	public void submitTool(PoseStack ms, SubmitNodeCollector queue, Vec3 camera) {
		super.submitTool(ms, queue, camera);
	}

	@Override
	public void renderOverlay(Gui gui, GuiGraphicsExtractor graphics, float partialTicks, int width, int height) {
		super.renderOverlay(gui, graphics, partialTicks, width, height);
	}

	@Override
	public boolean handleMouseWheel(double delta) {
		return false;
	}

	@Override
	public boolean handleRightClick() {
		return false;
	}

}
