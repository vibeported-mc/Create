package com.simibubi.create.content.schematics.client;

import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ToolSelectionScreen extends Screen {

	public final String scrollToCycle = CreateLang.translateDirect("gui.toolmenu.cycle")
		.getString();
	public final String holdToFocus = "gui.toolmenu.focusKey";

	protected List<ToolType> tools;
	protected Consumer<ToolType> callback;
	public boolean focused;
	private float yOffset;
	protected int selection;
	private boolean initialized;

	protected int w;
	protected int h;

	public ToolSelectionScreen(List<ToolType> tools, Consumer<ToolType> callback) {
		super(Component.literal("Tool Selection"));
		this.tools = tools;
		this.callback = callback;
		focused = false;
		yOffset = 0;
		selection = 0;
		initialized = false;

		callback.accept(tools.get(selection));

		w = Math.max(tools.size() * 50 + 30, 220);
		h = 30;
	}

	public void setSelectedElement(ToolType tool) {
		if (!tools.contains(tool))
			return;
		selection = tools.indexOf(tool);
	}

	public void cycle(int direction) {
		selection += (direction < 0) ? 1 : -1;
		selection = (selection + tools.size()) % tools.size();
	}

	private void draw(GuiGraphicsExtractor graphics, float partialTicks) {
		Matrix3x2fStack matrixStack = graphics.pose();
		Window mainWindow = minecraft.getWindow();
		if (!initialized)
			init(mainWindow.getGuiScaledWidth(), mainWindow.getGuiScaledHeight());

		int x = (mainWindow.getGuiScaledWidth() - w) / 2 + 15;
		int y = mainWindow.getGuiScaledHeight() - h - 75;

		matrixStack.pushMatrix();
		matrixStack.translate((float) (0), (float) (-yOffset));

		AllGuiTextures gray = AllGuiTextures.HUD_BACKGROUND;

		graphics.blit(RenderPipelines.GUI_TEXTURED, gray.location, x - 15, y, gray.getStartX(), gray.getStartY(), w, h, gray.getWidth(), gray.getHeight());

		float toolTipAlpha = yOffset / 10;
		List<Component> toolTip = tools.get(selection)
			.getDescription();
		int stringAlphaComponent = ((int) (toolTipAlpha * 0xFF)) << 24;

		if (toolTipAlpha > 0.25f) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, gray.location, x - 15, y + 33, gray.getStartX(), gray.getStartY(), w, h + 22, gray.getWidth(), gray.getHeight());

			if (toolTip.size() > 0)
				graphics.text(font, toolTip.get(0), x - 10, y + 38, 0xFFEEEEEE + stringAlphaComponent, false);
			if (toolTip.size() > 1)
				graphics.text(font, toolTip.get(1), x - 10, y + 50, 0xFFCCDDFF + stringAlphaComponent, false);
			if (toolTip.size() > 2)
				graphics.text(font, toolTip.get(2), x - 10, y + 60, 0xFFCCDDFF + stringAlphaComponent, false);
			if (toolTip.size() > 3)
				graphics.text(font, toolTip.get(3), x - 10, y + 72, 0xFFCCCCDD + stringAlphaComponent, false);
		}

		if (tools.size() > 1) {
			String keyName = AllKeys.TOOL_MENU.getBoundKey();
			int width = minecraft.getWindow()
				.getGuiScaledWidth();
			if (!focused)
				graphics.centeredText(minecraft.font, CreateLang.translateDirect(holdToFocus, keyName), width / 2,
					y - 10, 0xFFCCDDFF);
			else
				graphics.centeredText(minecraft.font, scrollToCycle, width / 2, y - 10, 0xFFCCDDFF);
		} else {
			x += 65;
		}


		for (int i = 0; i < tools.size(); i++) {
			matrixStack.pushMatrix();

			float alpha = focused ? 1 : .2f;
			if (i == selection) {
				matrixStack.translate((float) (0), (float) (-10));
				graphics.centeredText(minecraft.font, tools.get(i)
					.getDisplayName()
					.getString(), x + i * 50 + 24, y + 28, 0xFFCCDDFF);
				alpha = 1;
			}
			tools.get(i)
				.getIcon()
				.render(graphics, x + i * 50 + 16, y + 12);
			tools.get(i)
				.getIcon()
				.render(graphics, x + i * 50 + 16, y + 11);

			matrixStack.popMatrix();
		}

		matrixStack.popMatrix();
	}

	public void update() {
		if (focused)
			yOffset += (10 - yOffset) * .1f;
		else
			yOffset *= .9f;
	}

	public void renderPassive(GuiGraphicsExtractor graphics, float partialTicks) {
		draw(graphics, partialTicks);
	}

	@Override
	public void onClose() {
		callback.accept(tools.get(selection));
	}

	@Override
	protected void init() {
		super.init();
		initialized = true;
	}
}
