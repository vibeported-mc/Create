package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * The blur shown while cardboard armour is hiding the player.
 * <p>
 * This used to ride on the item's {@code renderHelmetOverlay}, which 26.2 dropped along with the rest
 * of the helmet overlay hook; it is a GUI layer of its own now, drawn the same way vanilla draws its
 * own full-screen overlays.
 */
public class CardboardArmorStealthOverlay implements GuiLayer {
	public static final CardboardArmorStealthOverlay INSTANCE = new CardboardArmorStealthOverlay();

	private static final Identifier PACKAGE_BLUR_LOCATION = Create.asResource("textures/misc/package_blur.png");

	private static LerpedFloat opacity = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, 0.25f, Chaser.EXP);

	public static void clientTick() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;

		opacity.tickChaser();
		opacity.updateChaseTarget(CardboardArmorHandler.testForStealth(player) ? 1 : 0);
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		float value = opacity.getValue(deltaTracker.getGameTimeDeltaPartialTick(false));
		if (value == 0)
			return;

		graphics.blit(RenderPipelines.GUI_TEXTURED, PACKAGE_BLUR_LOCATION, 0, 0, 0, 0, graphics.guiWidth(),
			graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight(), ARGB.white(value));
	}

}
