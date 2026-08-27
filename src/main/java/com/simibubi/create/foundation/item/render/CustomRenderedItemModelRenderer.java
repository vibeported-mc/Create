package com.simibubi.create.foundation.item.render;

import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Base for the items Create draws itself.
 * <p>
 * These used to be block-entity-without-level renderers, drawn immediately into a buffer source.
 * 26.2 replaced that with {@link SpecialModelRenderer}, which contributes to the frame's submit
 * queue from a layer of the item's render state.
 */
public abstract class CustomRenderedItemModelRenderer implements SpecialModelRenderer<CustomItemRenderContext> {

	/**
	 * The item's own cube, so the game can work out how much room to leave for it.
	 */
	private static final Vector3fc[] EXTENTS = {
		new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1),
		new Vector3f(1, 1, 0), new Vector3f(1, 0, 1), new Vector3f(0, 1, 1), new Vector3f(1, 1, 1) };

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		for (Vector3fc extent : EXTENTS)
			output.accept(extent);
	}

	@Override
	public @Nullable CustomItemRenderContext extractArgument(ItemStack stack) {
		return new CustomItemRenderContext(stack, ItemDisplayContext.NONE);
	}

	@Override
	public void submit(@Nullable CustomItemRenderContext context, PoseStack ms, SubmitNodeCollector collector,
		int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
		if (context == null)
			return;

		PartialItemModelRenderer renderer = PartialItemModelRenderer.of(context, ms, collector, overlayCoords);

		ms.pushPose();
		ms.translate(0.5F, 0.5F, 0.5F);
		render(context.stack(), renderer, context.displayContext(), ms, collector, lightCoords, overlayCoords);
		ms.popPose();
	}

	protected abstract void render(ItemStack stack, PartialItemModelRenderer renderer,
		ItemDisplayContext transformType, PoseStack ms, SubmitNodeCollector collector, int light, int overlay);

}
