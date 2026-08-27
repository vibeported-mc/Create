package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.compat.Mods;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Hooks into the minimap mods Create draws train positions on.
 * <p>
 * FTB Chunks and Xaero have no 26.2 builds yet, so the adapters they need are compiled out and the
 * hooks they had - a tooltip cancel and a GUI render, both FTB's - are gone with them. JourneyMap is
 * back, and Create's own train map is unaffected either way.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class TrainMapEvents {

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		if (Mods.JOURNEYMAP.isLoaded())
			JourneyTrainMap.tick();
	}

	@SubscribeEvent
	public static void mouseClick(InputEvent.MouseButton.Pre event) {
		if (event.getAction() != InputConstants.PRESS)
			return;

		if (Mods.JOURNEYMAP.isLoaded())
			JourneyTrainMap.mouseClick(event);
	}
}
