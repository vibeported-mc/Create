package com.simibubi.create.content.equipment.toolbox;

import org.joml.Matrix3x2fStack;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public class ToolboxScreen extends AbstractSimiContainerScreen<ToolboxMenu> {

	/** The colour vanilla's masked fill used to paint over a hovered slot. */
	private static final int SLOT_HIGHLIGHT_COLOR = 0x80_FFFFFF;

	protected static final AllGuiTextures BG = AllGuiTextures.TOOLBOX;
	protected static final AllGuiTextures PLAYER = AllGuiTextures.PLAYER_INVENTORY;

	protected Slot hoveredToolboxSlot;
	private IconButton confirmButton;
	private IconButton disposeButton;
	private DyeColor color;

	private List<Rect2i> extraAreas = Collections.emptyList();

	public ToolboxScreen(ToolboxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		init();
	}

	@Override
	protected void init() {
		setWindowSize(30 + BG.getWidth(), BG.getHeight() + PLAYER.getHeight() - 24);
		setWindowOffset(-11, 0);
		super.init();
		clearWidgets();

		color = menu.contentHolder.getColor();

		confirmButton = new IconButton(leftPos + 30 + BG.getWidth() - 33, topPos + BG.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			minecraft.player.closeContainer();
		});
		addRenderableWidget(confirmButton);

		disposeButton = new IconButton(leftPos + 30 + 81, topPos + 69, AllIcons.I_TOOLBOX);
		disposeButton.withCallback(() -> {
			ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxDisposeAllPacket(menu.contentHolder.getBlockPos()));
		});
		disposeButton.setToolTip(CreateLang.translateDirect("toolbox.depositBox"));
		addRenderableWidget(disposeButton);

		extraAreas = ImmutableList.of(
			new Rect2i(leftPos + 30 + BG.getWidth(), topPos + BG.getHeight() - 15 - 34 - 6, 72, 68)
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		menu.renderPass = true;
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		menu.renderPass = false;
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int x = leftPos + imageWidth - BG.getWidth();
		int y = topPos;

		BG.render(graphics, x, y);
		graphics.text(font, title, x + 15, y + 4, 0x592424, false);

		int invX = leftPos;
		int invY = topPos + imageHeight - PLAYER.getHeight();
		renderPlayerInventory(graphics, invX, invY);

		renderToolbox(graphics, x + BG.getWidth() + 50, y + BG.getHeight() + 12, partialTicks);

		Matrix3x2fStack ms = graphics.pose();

		hoveredToolboxSlot = null;
		for (int compartment = 0; compartment < 8; compartment++) {
			int baseIndex = compartment * ToolboxInventory.STACKS_PER_COMPARTMENT;
			Slot slot = menu.slots.get(baseIndex);
			ItemStack itemstack = slot.getItem();
			int i = slot.x + leftPos;
			int j = slot.y + topPos;

			if (itemstack.isEmpty())
				itemstack = menu.getFilter(compartment);

			if (!itemstack.isEmpty()) {
				int count = menu.totalCountInCompartment(compartment);
				String s = String.valueOf(count);
				ms.pushMatrix();
				ms.translate((float) (0), (float) (0));
				graphics.item(minecraft.player, itemstack, i, j, 0);
				graphics.itemDecorations(font, itemstack, i, j, s);
				ms.popMatrix();
			}

			if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
				hoveredToolboxSlot = slot;
				graphics.fillGradient(i, j, i + 16, j + 16, SLOT_HIGHLIGHT_COLOR, SLOT_HIGHLIGHT_COLOR);
			}
		}
	}

	private void renderToolbox(GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
		Matrix3x2fStack ms = graphics.pose();
		ms.pushMatrix();
		ms.translate(x, y);
		// A block is 16 units wide inside a picture-in-picture texture; the toolbox was drawn 50 wide.
		ms.scale(50 / 16f, 50 / 16f);

		GuiGameElement.of(AllBlocks.TOOLBOXES.get(color)
			.getDefaultState())
			.viewRotate(-22, -202, 0)
			.submit(graphics);

		float lid = menu.contentHolder.lid.getValue(partialTicks);
		GuiGameElement.of(AllPartialModels.TOOLBOX_LIDS.get(color).get())
			.viewRotate(-22, -202, 0)
			.rotate(-105 * lid, 0, 0)
			.withRotationOffset(0, -6 / 16f, 12 / 16f)
			.submit(graphics);

		float drawers = menu.contentHolder.drawers.getValue(partialTicks);
		for (int offset : Iterate.zeroAndOne)
			GuiGameElement.of(AllPartialModels.TOOLBOX_DRAWER.get())
				.viewRotate(-22, -202, 0)
				.atLocal(0, -offset * 1 / 8f, drawers * -.175f * (2 - offset))
				.submit(graphics);

		ms.popMatrix();
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (hoveredToolboxSlot != null)
			hoveredSlot = hoveredToolboxSlot;
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
