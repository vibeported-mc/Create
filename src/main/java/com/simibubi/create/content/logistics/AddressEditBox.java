package com.simibubi.create.content.logistics;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.schedule.DestinationSuggestions;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AddressEditBox extends EditBox {

	private DestinationSuggestions destinationSuggestions;
	private Consumer<String> mainResponder;
	private String prevValue = "=)";

	public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom) {
		this(screen, pFont, pX, pY, pWidth, pHeight, anchorToBottom, null);
	}

	public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom, String localAddress) {
		super(pFont, pX, pY, pWidth, pHeight, Component.empty());
		destinationSuggestions = AddressEditBoxHelper.createSuggestions(screen, this, anchorToBottom, localAddress);
		destinationSuggestions.setAllowSuggestions(true);
		destinationSuggestions.updateCommandInfo();
		mainResponder = t -> {
			if (!t.equals(prevValue))
				destinationSuggestions.updateCommandInfo();
			prevValue = t;
		};
		setResponder(mainResponder);
		setBordered(false);
		setFocused(false);
		mouseClicked(0, 0, 0);
		setMaxLength(25);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int pKeyCode = event.key();
		int pScanCode = event.scancode();
		int pModifiers = event.modifiers();
		if (destinationSuggestions.keyPressed(event))
			return true;
		if (isFocused() && pKeyCode == GLFW.GLFW_KEY_ENTER) {
			setFocused(false);
			moveCursorToEnd(false);
			mouseClicked(0, 0, 0);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (destinationSuggestions.mouseScrolled(Mth.clamp(scrollY, -1.0D, 1.0D)))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double pMouseX = event.x();
		double pMouseY = event.y();
		int pButton = event.button();
		if (pButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			if (isMouseOver(pMouseX, pMouseY)) {
				setValue("");
				return true;
			}
		}

		boolean wasFocused = isFocused();
		if (super.mouseClicked(event, doubleClick)) {
			if (!wasFocused) {
				setHighlightPos(0);
				setCursorPosition(getValue().length());
			}
			return true;
		}
		if (destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
			return true;
		return false;
	}

	@Override
	public void setValue(String text) {
		setHighlightPos(0);
		super.setValue(text);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
	}

	@Override
	public void renderWidget(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		Matrix3x2fStack matrixStack = pGuiGraphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (0), (float) (0));
		destinationSuggestions.render(pGuiGraphics, pMouseX, pMouseY);
		matrixStack.popMatrix();

		if (!destinationSuggestions.isEmpty())
			return;

		int itemX = getX() + width + 4;
		int itemY = getY() - 4;
		pGuiGraphics.item(AllBlocks.CLIPBOARD.asStack(), itemX, itemY);
		if (pMouseX >= itemX && pMouseX < itemX + 16 && pMouseY >= itemY && pMouseY < itemY + 16) {
			List<Component> promiseTip = List.of();
			promiseTip = List.of(CreateLang.translate("gui.address_box.clipboard_tip")
					.color(ScrollInput.HEADER_RGB)
					.component(),
				CreateLang.translate("gui.address_box.clipboard_tip_1")
					.style(ChatFormatting.GRAY)
					.component(),
				CreateLang.translate("gui.address_box.clipboard_tip_2")
					.style(ChatFormatting.GRAY)
					.component(),
				CreateLang.translate("gui.address_box.clipboard_tip_3")
					.style(ChatFormatting.GRAY)
					.component(),
				CreateLang.translate("gui.address_box.clipboard_tip_4")
					.style(ChatFormatting.DARK_GRAY)
					.component());
			pGuiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, promiseTip, pMouseX, pMouseY);
		}
	}

	@Override
	public void setResponder(Consumer<String> pResponder) {
		super.setResponder(pResponder == mainResponder ? mainResponder : mainResponder.andThen(pResponder));
	}

	public void tick() {
		if (!isFocused())
			destinationSuggestions.hide();
		if (isFocused() && destinationSuggestions.suggestions == null)
			destinationSuggestions.updateCommandInfo();
		destinationSuggestions.tick();
	}
}
