package com.simibubi.create.compat.sodium;

import com.simibubi.create.Create;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Fixes the Mechanical Saw's sprite and Factory Gauge's sprite
 */
public class SodiumCompat {
	public static final Identifier SAW_TEXTURE = Create.asResource("block/saw_reversed");
	public static final Identifier FACTORY_PANEL_TEXTURE = Create.asResource("block/factory_panel_connections_animated");

	public static void init(IEventBus modEventBus, IEventBus neoEventBus) {
		Minecraft mc = Minecraft.getInstance();

		// RenderLevelStageEvent no longer carries a Stage enum; each stage is its own event class, and
		// AfterOpaqueFeatures is the one that fires once entities and block entities have been drawn.
		neoEventBus.addListener((RenderLevelStageEvent.AfterOpaqueFeatures event) -> {
			// Atlases are reached through the AtlasManager now, and are keyed by their definition id
			// rather than by the texture path the old InventoryMenu constant held.
			TextureAtlas atlas = mc.getAtlasManager()
				.getAtlasOrThrow(AtlasIds.BLOCKS);

			TextureAtlasSprite sawSprite = atlas.getSprite(SAW_TEXTURE);
			TextureAtlasSprite factoryPanelSprite = atlas.getSprite(FACTORY_PANEL_TEXTURE);

			SpriteUtil.INSTANCE.markSpriteActive(sawSprite);
			SpriteUtil.INSTANCE.markSpriteActive(factoryPanelSprite);
		});
	}
}
