package com.simibubi.create.content.equipment.hats;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import com.simibubi.create.foundation.render.CachedBufferer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sits a hat on an entity's head.
 * <p>
 * A render layer works off the entity's render state in 26.2, and every model is reached through one
 * root part, so the head is found by walking that root rather than by asking each model shape for
 * its head parts.
 */
public class CreateHatArmorLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
	extends RenderLayer<S, M> {

	public CreateHatArmorLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack ms, SubmitNodeCollector queue, int light, S state, float yRot, float xRot) {
		HatRenderData data = HatRenderData.of(state);
		if (data == null)
			return;

		PartialModel hat = data.hat();
		TrainHatInfo info = data.info();
		List<ModelPart> partsToHead = TrainHatInfo.getAdjustedPart(info, getParentModel().root(), "head");
		if (partsToHead.isEmpty())
			return;

		ms.pushPose();
		partsToHead.forEach(part -> part.translateAndRotate(ms));

		ModelPart lastChild = partsToHead.get(partsToHead.size() - 1);
		if (!lastChild.isEmpty()) {
			Cube cube = lastChild.cubes.get(Mth.clamp(info.cubeIndex(), 0, lastChild.cubes.size() - 1));
			ms.translate(info.offset().x() / 16.0F, (cube.minY - cube.maxY + info.offset().y()) / 16.0F,
				info.offset().z() / 16.0F);
			float max = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8.0F * info.scale();
			ms.scale(max, max, max);
		}

		ms.scale(1, -1, -1);
		ms.translate(0, -2.25F / 16.0F, 0);

		BlockState air = Blocks.AIR.defaultBlockState();
		SuperByteBuffer buffer = CachedBufferer.partial(hat, air);
		buffer.rotateXDegrees(-8.5F)
			.disableDiffuse()
			.light(light)
			.extractRenderState()
			.submit(ms, RenderTypes.cutoutMovingBlock(), queue);

		ms.popPose();
	}

	public static void registerOnAll(EntityRenderDispatcher renderManager) {
		for (EntityRenderer<? extends Player, ?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getPlayerRenderers()
			.values())
			registerOn(renderer);
		for (EntityRenderer<?, ?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getRenderers()
			.values())
			registerOn(renderer);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void registerOn(EntityRenderer<?, ?> entityRenderer) {
		if (!(entityRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer))
			return;
		CreateHatArmorLayer<?, ?> layer = new CreateHatArmorLayer<>(livingRenderer);
		livingRenderer.addLayer((CreateHatArmorLayer) layer);
	}

}
