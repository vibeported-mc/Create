package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class TableClothRenderer
	extends SmartBlockEntityRenderer<TableClothBlockEntity, TableClothRenderer.TableClothRenderState> {

	public static class TableClothRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState priceTag;
		public final List<DepotRenderer.ItemState> stacks = new ArrayList<>();
		public float rotationInRadians;
		public Vec3 itemPosition = Vec3.ZERO;
	}

	public TableClothRenderer(Context context) {
		super(context);
	}

	@Override
	public TableClothRenderState createRenderState() {
		return new TableClothRenderState();
	}

	@Override
	protected void extractSafe(TableClothBlockEntity be, TableClothRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.priceTag = null;
		state.stacks.clear();

		state.rotationInRadians = Mth.DEG_TO_RAD * (180 - be.facing.toYRot());
		state.itemPosition = Vec3.atCenterOf(be.getBlockPos());

		if (be.isShop()) {
			var priceTag = CachedBufferer.partial(be.sideOccluded ? AllPartialModels.TABLE_CLOTH_PRICE_TOP
				: AllPartialModels.TABLE_CLOTH_PRICE_SIDE, be.getBlockState());
			TransformStack.of(priceTag.getTransforms())
				.rotateCentered(state.rotationInRadians, Direction.UP);
			state.priceTag = priceTag.light(state.lightCoords)
				.extractRenderState();
		}

		for (ItemStack entry : be.getItemsForRender())
			state.stacks.add(DepotRenderer.ItemState.create(itemModelResolver, entry, be.getLevel()));
	}

	@Override
	protected void submitSafe(TableClothRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		if (state.priceTag != null)
			state.priceTag.submit(ms, RenderTypes.cutoutMovingBlock(), queue);

		int count = state.stacks.size();
		ms.pushPose();
		TransformStack.of(ms)
			.rotateCentered(state.rotationInRadians, Direction.UP);
		for (int i = 0; i < count; i++) {
			DepotRenderer.ItemState entry = state.stacks.get(i);
			ms.pushPose();
			ms.translate(0.5f, 3 / 16f, 0.5f);

			if (count > 1) {
				ms.mulPose(Axis.YP.rotationDegrees(i * (360f / count) + 45f));
				ms.translate(0, i % 2 == 0 ? -0.005 : 0, 5 / 16f);
				ms.mulPose(Axis.YP.rotationDegrees(-i * (360f / count) - 45f));
			}

			// Flat items lie facing the shopper rather than following the cloth's rotation.
			if (!entry.blockItem())
				TransformStack.of(ms)
					.rotate(-state.rotationInRadians + Mth.PI, Direction.UP);

			DepotRenderer.submitItem(ms, queue, state.lightCoords, entry, 0, null, state.itemPosition, true);
			ms.popPose();
		}

		ms.popPose();
	}

}
