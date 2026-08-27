package com.simibubi.create.content.logistics.filter;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PackageFilterScreen extends AbstractFilterScreen<PackageFilterMenu> {

	private AddressEditBox addressBox;
	private boolean deferFocus;

	public PackageFilterScreen(PackageFilterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, AllGuiTextures.PACKAGE_FILTER);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (deferFocus) {
			deferFocus = false;
			setFocused(addressBox);
		}
		addressBox.tick();
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 7);
		super.init();

		int x = leftPos;
		int y = topPos;

		addressBox = new AddressEditBox(this, this.font, x + 44, y + 28, 129, 9, false);
		addressBox.setTextColor(0xffffff);
		addressBox.setValue(menu.address);
		addressBox.setResponder(this::onAddressEdited);
		addRenderableWidget(addressBox);

		setFocused(addressBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		Matrix3x2fStack ms = graphics.pose();
		ms.pushMatrix();
		ms.translate((float) (leftPos + 16), (float) (topPos + 23));
		GuiGameElement.of(PackageStyles.getDefaultBox())
			.submit(graphics);
		ms.popMatrix();
	}

	public void onAddressEdited(String s) {
		menu.address = s;
		CompoundTag tag = new CompoundTag();
		tag.putString("Address", s);
		ClientNetworkHelper.INSTANCE.sendToServer(new FilterScreenPacket(Option.UPDATE_ADDRESS, tag));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double pMouseX = event.x();
		double pMouseY = event.y();
		int pButton = event.button();
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int pKeyCode = event.key();
		int pScanCode = event.scancode();
		int pModifiers = event.modifiers();
		if (pKeyCode == GLFW.GLFW_KEY_ENTER)
			setFocused(null);
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char pCodePoint = (char) event.codepoint();
		int pModifiers = 0;
		return super.charTyped(event);
	}

	@Override
	protected void contentsCleared() {
		addressBox.setValue("");
		deferFocus = true;
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		return false;
	}

	@Override
	protected int getTitleColor() {
		return 0x3D3C48;
	}
}
