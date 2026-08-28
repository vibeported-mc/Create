package com.simibubi.create.infrastructure.gui;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.resources.Identifier;

/**
 * Create's own spinning panorama for its main menu.
 * <p>
 * Minecraft 26.2 moved the panorama out of the screen: a screen only asks for one while extracting its
 * frame, and the GUI renderer draws a single cube map it owns itself. Create's panorama therefore can't
 * be a second {@code PanoramaRenderer} any more — the screen flags that it wants Create's cube map, and
 * {@code GuiRendererMixin} swaps it in for that one frame.
 */
public class CreatePanorama {

	public static final Identifier CUBE_MAP_LOCATION = Create.asResource("textures/gui/title/background/panorama");

	private static @Nullable CubeMap cubeMap;
	private static boolean requested;

	/**
	 * Requests Create's panorama for the frame being extracted, and the overlay that sits on top of it.
	 */
	public static void extract(GuiGraphicsExtractor graphics, int width, int height) {
		requested = true;
		Minecraft.getInstance()
			.gameRenderer.panorama()
			.extractRenderState(graphics, width, height);
	}

	/**
	 * Registers the panorama's textures, which the texture manager only uploads on a resource reload -
	 * so this has to happen before the menu is ever drawn, not on the frame that first asks for it.
	 */
	public static void registerTextures() {
		cubeMap = new CubeMap(CUBE_MAP_LOCATION);
		cubeMap.registerTextures(Minecraft.getInstance()
			.getTextureManager());
	}

	/**
	 * Picks the cube map to draw this frame, consuming the request made while extracting it.
	 */
	public static CubeMap pick(CubeMap vanilla) {
		if (!requested)
			return vanilla;
		requested = false;
		return cubeMap == null ? vanilla : cubeMap;
	}

	private CreatePanorama() {
	}

}
