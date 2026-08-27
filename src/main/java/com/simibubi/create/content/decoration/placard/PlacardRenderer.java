package com.simibubi.create.content.decoration.placard;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class PlacardRenderer extends SafeBlockEntityRenderer<PlacardBlockEntity, PlacardRenderer.PlacardRenderState> {

	public static class PlacardRenderState extends SafeRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public @Nullable Direction facing;
		public @Nullable AttachFace face;
		public boolean blockItem;
	}

	private final ItemModelResolver itemModelResolver;

	public PlacardRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public PlacardRenderState createRenderState() {
		return new PlacardRenderState();
	}

	@Override
	protected void extractSafe(PlacardBlockEntity be, PlacardRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		ItemStack heldItem = be.getHeldItem();
		if (heldItem.isEmpty()) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		state.facing = blockState.getValue(PlacardBlock.FACING);
		state.face = blockState.getValue(PlacardBlock.FACE);

		itemModelResolver.updateForTopItem(state.item, heldItem, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
		// A block-shaped model wants the larger of the two scales, and its lighting is what tells it apart.
		state.blockItem = state.item.usesBlockLight();
	}

	@Override
	protected void submitSafe(PlacardRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.facing == null || state.face == null)
			return;

		AttachFace face = state.face;

		ms.pushPose();
		TransformStack.of(ms)
			.center()
			.rotate(
				(face == AttachFace.CEILING ? Mth.PI : 0)
					+ AngleHelper.rad(180 + AngleHelper.horizontalAngle(state.facing)),
				Direction.UP)
			.rotate(face == AttachFace.CEILING ? -Mth.PI / 2 : face == AttachFace.FLOOR ? Mth.PI / 2 : 0,
				Direction.EAST)
			.translate(0, 0, 4.5 / 16f)
			.scale(state.blockItem ? .5f : .375f);

		state.item.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

}
