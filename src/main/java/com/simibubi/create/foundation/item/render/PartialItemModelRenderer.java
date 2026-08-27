package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.render.RenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Draws pieces of an item's model, one partial at a time.
 * <p>
 * 26.2 builds a frame by submitting to a queue rather than writing into a buffer source, so each
 * piece is submitted as custom geometry; the item's own base model goes through the item render
 * state the way vanilla draws it.
 */
public class PartialItemModelRenderer {

	private static final PartialItemModelRenderer INSTANCE = new PartialItemModelRenderer();

	private final RandomSource random = RandomSource.create();
	private final ItemStackRenderState scratchState = new ItemStackRenderState();

	private CustomItemRenderContext context;
	private PoseStack ms;
	private SubmitNodeCollector collector;
	private int overlay;

	public static PartialItemModelRenderer of(CustomItemRenderContext context, PoseStack ms,
		SubmitNodeCollector collector, int overlay) {
		PartialItemModelRenderer instance = INSTANCE;
		instance.context = context;
		instance.ms = ms;
		instance.collector = collector;
		instance.overlay = overlay;
		return instance;
	}

	public void render(BlockStateModel model, int light) {
		render(model, Sheets.translucentBlockItemSheet(), light);
	}

	public void renderSolid(BlockStateModel model, int light) {
		render(model, Sheets.cutoutBlockItemSheet(), light);
	}

	public void renderGlowing(BlockStateModel model, int light) {
		render(model, RenderTypes.itemGlowingTranslucent(), light);
	}

	public void renderSolidGlowing(BlockStateModel model, int light) {
		render(model, RenderTypes.itemGlowingSolid(), light);
	}

	public void render(BlockStateModel model, RenderType type, int light) {
		ItemStack stack = context.stack();
		if (stack.isEmpty())
			return;

		random.setSeed(42L);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(random, parts);
		List<BakedQuad> quads = collectQuads(parts);
		if (quads.isEmpty())
			return;

		ms.pushPose();
		ms.translate(-0.5D, -0.5D, -0.5D);
		QuadInstance instance = new QuadInstance();
		instance.setLightCoords(light);
		instance.setOverlayCoords(overlay);
		collector.submitCustomGeometry(ms, type, (pose, buffer) -> {
			for (BakedQuad quad : quads)
				buffer.putBakedQuad(pose, quad, instance);
		});
		ms.popPose();
	}

	/**
	 * Draws the item's own model, the one Create's wrapper took the place of.
	 */
	public void renderBase(int light) {
		ItemModel base = context.baseModel();
		if (base == null)
			return;

		ms.pushPose();
		ms.translate(-0.5D, -0.5D, -0.5D);
		scratchState.clear();
		base.update(scratchState, context.stack(), Minecraft.getInstance()
			.getItemModelResolver(), context.displayContext(), context.level(), context.owner(), context.seed());
		scratchState.submit(ms, collector, light, overlay, 0);
		ms.popPose();
	}

	private static List<BakedQuad> collectQuads(List<BlockStateModelPart> parts) {
		List<BakedQuad> quads = new ArrayList<>(BakedModelHelper.quadsOf(parts, null));
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values())
			quads.addAll(BakedModelHelper.quadsOf(parts, direction));
		return quads;
	}

}
