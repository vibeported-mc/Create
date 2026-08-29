package com.simibubi.create.foundation.gui.menu;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NullMarked;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.TickableGuiEventListener;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NullMarked
@OnlyIn(Dist.CLIENT)
public abstract class AbstractSimiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

	protected int windowXOffset, windowYOffset;

	public AbstractSimiContainerScreen(T container, Inventory inv, Component title) {
		super(container, inv, title);
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		imageWidth = width;
		imageHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		super.init();
		leftPos += windowXOffset;
		topPos += windowYOffset;
	}

	@Override
	protected void containerTick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	/**
	 * 26.2 folded {@code renderBg} into the screen's background extraction, but Create's screens are
	 * written around it, so it stays as the hook they override.
	 */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);
		renderBg(graphics, partialTicks, mouseX, mouseY);
	}

	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		// The frame delta a screen is handed is not the accumulated tick fraction animations need.
		partialTicks = AnimationTickHolder.getGuiPartialTicks();

		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// no-op to prevent screen- and inventory-title from being rendered at incorrect
		// location
		// could also set this.titleX/Y and this.playerInventoryTitleX/Y to the proper
		// values instead
	}

	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		extractTooltip(graphics, mouseX, mouseY);
		for (Renderable widget : renderables) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isMouseOver(mouseX, mouseY)) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					continue;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.setComponentTooltipForNextFrame(font, tooltip, ttx, tty);
			}
		}
	}

	public int getLeftOfCentered(int textureWidth) {
		return leftPos - windowXOffset + (imageWidth - textureWidth) / 2;
	}

	public void renderPlayerInventory(GuiGraphicsExtractor graphics, int x, int y) {
		AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y);
		graphics.text(font, playerInventoryTitle, x + 8, y + 6, 0xFF404040, false);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int pKeyCode = event.key();
		int pScanCode = event.scancode();
		int pModifiers = event.modifiers();
		if (getFocused() instanceof EditBox && pKeyCode != GLFW.GLFW_KEY_ESCAPE)
			return getFocused().keyPressed(event);
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double pMouseX = event.x();
		double pMouseY = event.y();
		int pButton = event.button();
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(event, doubleClick);
	}

	/**
	 * 26.2's AbstractContainerScreen answers a scroll itself and stops there, where it used to fall
	 * through to whatever widget the cursor was over. Create's screens are full of widgets that are
	 * driven by scrolling, so hand the event on when the slot actions do not want it.
	 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return getChildAt(mouseX, mouseY).filter(child -> child.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			.isPresent();
	}

	@Override
	public GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !((AbstractWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

	/**
	 * Used for moving JEI out of the way of extra things like block renders.
	 *
	 * @return the space that the GUI takes up outside the normal rectangle defined
	 *         by {@link ContainerScreen}.
	 */
	public List<Rect2i> getExtraAreas() {
		return Collections.emptyList();
	}

	@Deprecated
	protected void debugWindowArea(GuiGraphicsExtractor graphics) {
		graphics.fill(leftPos + imageWidth, topPos + imageHeight, leftPos, topPos, 0xD3D3D3D3);
	}

	@Deprecated
	protected void debugExtraAreas(GuiGraphicsExtractor graphics) {
		for (Rect2i area : getExtraAreas()) {
			graphics.fill(area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(),
				0xD3D3D3D3);
		}
	}

	protected void playUiSound(SoundEvent sound, float volume, float pitch) {
		Minecraft.getInstance()
			.getSoundManager()
			.play(SimpleSoundInstance.forUI(sound, pitch, volume * 0.25f));
	}

}
