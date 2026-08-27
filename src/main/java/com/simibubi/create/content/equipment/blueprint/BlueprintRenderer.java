package com.simibubi.create.content.equipment.blueprint;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity.BlueprintSection;
import com.simibubi.create.foundation.render.CachedBufferer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Draws a crafting blueprint and the items pinned to it.
 * <p>
 * Minecraft 26.2 splits rendering into an extraction pass and a submit pass, so the blueprint's
 * geometry and each pinned item's render state are worked out up front and only replayed here.
 */
public class BlueprintRenderer extends EntityRenderer<BlueprintEntity, BlueprintRenderer.BlueprintRenderState> {

	public static class BlueprintRenderState extends EntityRenderState {
		public @Nullable SuperByteBufferRenderState board;
		public final List<PinnedItem> items = new ArrayList<>();
		public int itemLight;
	}

	/**
	 * One item on the board, with the transform that places it.
	 */
	public record PinnedItem(ItemStackRenderState stack, PoseStack placement) {
	}

	public BlueprintRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BlueprintRenderState createRenderState() {
		return new BlueprintRenderState();
	}

	@Override
	public void extractRenderState(BlueprintEntity entity, BlueprintRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.items.clear();

		float yaw = entity.getYRot();
		PartialModel partialModel = entity.size == 3 ? AllPartialModels.CRAFTING_BLUEPRINT_3x3
			: entity.size == 2 ? AllPartialModels.CRAFTING_BLUEPRINT_2x2 : AllPartialModels.CRAFTING_BLUEPRINT_1x1;

		SuperByteBuffer sbb = CachedBufferer.partial(partialModel, Blocks.AIR.defaultBlockState());
		sbb.rotateYDegrees(-yaw)
			.rotateXDegrees(90.0F + entity.getXRot())
			.translate(-.5, -1 / 32f, -.5);
		if (entity.size == 2)
			sbb.translate(.5, 0, -.5);
		state.board = sbb.disableDiffuse()
			.light(state.lightCoords)
			.extractRenderState();

		// The board catches light as if it faced the viewer, so the items on it are lit to match.
		float fakeNormalXRotation = -15;
		int bl = state.lightCoords >> 4 & 0xf;
		int sl = state.lightCoords >> 20 & 0xf;
		boolean vertical = entity.getXRot() != 0;
		if (entity.getXRot() == -90)
			fakeNormalXRotation = -45;
		else if (entity.getXRot() == 90 || yaw % 180 != 0) {
			bl /= 1.35;
			sl /= 1.35;
		}
		state.itemLight = Mth.floor(sl + .5) << 20 | (Mth.floor(bl + .5) & 0xf) << 4;

		PoseStack normalMs = new PoseStack();
		TransformStack.of(normalMs)
			.rotateYDegrees(vertical ? 0 : -yaw)
			.rotateXDegrees(fakeNormalXRotation);
		Matrix3f flatNormal = new Matrix3f(normalMs.last()
			.normal());

		PoseStack ms = new PoseStack();
		TransformStack.of(ms)
			.rotateYDegrees(-yaw)
			.rotateXDegrees(entity.getXRot())
			.translate(0, 0, 1 / 32f + .001);
		if (entity.size == 3)
			ms.translate(-1, -1, 0);

		PoseStack squashedMS = new PoseStack();
		squashedMS.last()
			.pose()
			.mul(ms.last()
				.pose());

		Minecraft mc = Minecraft.getInstance();
		for (int x = 0; x < entity.size; x++) {
			squashedMS.pushPose();
			for (int y = 0; y < entity.size; y++) {
				BlueprintSection section = entity.getSection(x * entity.size + y);
				Couple<ItemStack> displayItems = section.getDisplayItems();
				squashedMS.pushPose();
				squashedMS.scale(.5f, .5f, 1 / 1024f);
				displayItems.forEachWithContext((stack, primary) -> {
					if (stack.isEmpty())
						return;

					squashedMS.pushPose();
					if (!primary) {
						squashedMS.translate(0.325f, -0.325f, 1);
						squashedMS.scale(.625f, .625f, 1);
					}

					squashedMS.last()
						.normal()
						.set(flatNormal);

					ItemStackRenderState itemState = new ItemStackRenderState();
					mc.getItemModelResolver()
						.updateForTopItem(itemState, stack, ItemDisplayContext.GUI, entity.level(), null, 0);

					PoseStack placement = new PoseStack();
					placement.last()
						.set(squashedMS.last());
					state.items.add(new PinnedItem(itemState, placement));

					squashedMS.popPose();
				});
				squashedMS.popPose();
				squashedMS.translate(1, 0, 0);
			}
			squashedMS.popPose();
			squashedMS.translate(0, 1, 0);
		}
	}

	@Override
	public void submit(BlueprintRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.board != null)
			state.board.submit(ms, RenderTypes.solidMovingBlock(), queue);
		super.submit(state, ms, queue, camera);

		for (PinnedItem item : state.items) {
			ms.pushPose();
			ms.last()
				.set(item.placement()
					.last());
			item.stack()
				.submit(ms, queue, state.itemLight, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	public Identifier getTextureLocation(BlueprintEntity entity) {
		return null;
	}

}
