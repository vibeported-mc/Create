package com.simibubi.create.content.logistics.box;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class PackageRenderer extends EntityRenderer<PackageEntity, PackageRenderer.PackageRenderState> {

	public static class PackageRenderState extends EntityRenderState {
		public @Nullable SuperByteBufferRenderState box;
	}

	public PackageRenderer(Context pContext) {
		super(pContext);
		shadowRadius = 0.5f;
	}

	@Override
	public PackageRenderState createRenderState() {
		return new PackageRenderState();
	}

	@Override
	public void extractRenderState(PackageEntity entity, PackageRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.box = null;

		if (VisualizationManager.supportsVisualization(entity.level()))
			return;

		ItemStack box = entity.box;
		if (box.isEmpty() || !PackageItem.isPackage(box))
			box = AllBlocks.CARDBOARD_BLOCK.asStack();
		PartialModel model = AllPartialModels.PACKAGES.get(BuiltInRegistries.ITEM.getKey(box.getItem()));
		state.box = extractBox(entity, entity.getYRot(partialTicks), state.lightCoords, model);
	}

	@Override
	public void submit(PackageRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.box != null)
			state.box.submit(ms, RenderTypes.solidMovingBlock(), queue);
		super.submit(state, ms, queue, camera);
	}

	/**
	 * A package's box, already turned to face its yaw. Cardboard armour draws the same geometry, so
	 * this stays shared.
	 */
	public static @Nullable SuperByteBufferRenderState extractBox(Entity entity, float yaw, int light,
		@Nullable PartialModel model) {
		if (model == null)
			return null;
		SuperByteBuffer sbb = CachedBufferer.partial(model, Blocks.AIR.defaultBlockState());
		return sbb.translate(-.5, 0, -.5)
			.rotateCentered(-AngleHelper.rad(yaw + 90), Direction.UP)
			.light(light)
			.nudge(entity.getId())
			.extractRenderState();
	}

}
