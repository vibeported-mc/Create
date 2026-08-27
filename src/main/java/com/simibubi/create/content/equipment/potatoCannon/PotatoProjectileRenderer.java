package com.simibubi.create.content.equipment.potatoCannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileRenderMode;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PotatoProjectileRenderer
	extends EntityRenderer<PotatoProjectileEntity, PotatoProjectileRenderer.PotatoProjectileRenderState> {

	public static class PotatoProjectileRenderState extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public boolean empty = true;
		public double halfHeight;
		public @Nullable PotatoProjectileRenderMode mode;
		public PotatoProjectileRenderMode.Context context =
			new PotatoProjectileRenderMode.Context(Vec3.ZERO, Vec3.ZERO, 0, 0);
	}

	private final ItemModelResolver itemModelResolver;

	public PotatoProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public PotatoProjectileRenderState createRenderState() {
		return new PotatoProjectileRenderState();
	}

	@Override
	public void extractRenderState(PotatoProjectileEntity entity, PotatoProjectileRenderState state,
		float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);

		ItemStack item = entity.getItem();
		state.empty = item.isEmpty();
		if (state.empty)
			return;

		itemModelResolver.appendItemLayers(state.item, item, ItemDisplayContext.GROUND, entity.level(), null, 0);
		state.halfHeight = entity.getBoundingBox()
			.getYsize() / 2;

		// The render mode only applies transforms, so it runs during submission off this snapshot.
		state.mode = entity.getRenderMode();
		Vec3 toCamera = entity.getBoundingBox()
			.getCenter()
			.subtract(entityRenderDispatcher.camera.getPosition());
		state.context = new PotatoProjectileRenderMode.Context(toCamera, entity.getDeltaMovement(),
			entity.tickCount + partialTicks, System.identityHashCode(entity));
	}

	@Override
	public void submit(PotatoProjectileRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.empty)
			return;

		ms.pushPose();
		ms.translate(0, state.halfHeight - 1 / 8f, 0);
		if (state.mode != null)
			state.mode.transform(ms, state.context);

		state.item.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

}
