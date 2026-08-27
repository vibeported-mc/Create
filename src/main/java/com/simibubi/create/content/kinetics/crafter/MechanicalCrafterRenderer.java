package com.simibubi.create.content.kinetics.crafter;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer.standardKineticRotationTransform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity.Phase;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.Pointing;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MechanicalCrafterRenderer
	extends SafeBlockEntityRenderer<MechanicalCrafterBlockEntity, MechanicalCrafterRenderer.CrafterRenderState> {

	public static class CrafterRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(4);
		public final List<GridItem> items = new ArrayList<>();
		public final List<ItemStackRenderState> craftResult = new ArrayList<>();
		public Vec3 itemAnchor = Vec3.ZERO;
		public Vec3 centering = Vec3.ZERO;
		public float yRot;
		public float spacing = .5f;
		public float craftingScale = 1;
		public float resultSpin;
		public float resultScale = 1;
		public boolean idle;
	}

	/** One item in the crafting grid; zNudge keeps overlapping stacks from z-fighting. */
	public record GridItem(ItemStackRenderState item, int x, int y, float zNudge) {
	}

	protected final ItemModelResolver itemModelResolver;

	public MechanicalCrafterRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public CrafterRenderState createRenderState() {
		return new CrafterRenderState();
	}

	@Override
	protected void extractSafe(MechanicalCrafterBlockEntity be, CrafterRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.parts.clear();
		state.items.clear();
		state.craftResult.clear();

		Direction facing = be.getBlockState()
			.getValue(HORIZONTAL_FACING);
		Vec3 vec = Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(.58)
			.add(.5, .5, .5);

		if (be.phase == Phase.EXPORTING) {
			Direction targetDirection = MechanicalCrafterBlock.getTargetDirection(be.getBlockState());
			float progress = Mth.clamp((1000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
			vec = vec.add(Vec3.atLowerCornerOf(targetDirection.getUnitVec3i())
				.scale(progress * .75f));
		}

		state.itemAnchor = vec;
		state.yRot = AngleHelper.horizontalAngle(facing);

		extractItems(be, state, partialTicks);
		extractStatic(be, state);
	}

	@Override
	protected void submitSafe(CrafterRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		ms.pushPose();
		ms.translate(state.itemAnchor.x, state.itemAnchor.y, state.itemAnchor.z);
		ms.scale(1 / 2f, 1 / 2f, 1 / 2f);
		ms.mulPose(Axis.YP.rotationDegrees(state.yRot));
		submitItems(state, ms, queue);
		ms.popPose();

		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public void extractItems(MechanicalCrafterBlockEntity be, CrafterRenderState state, float partialTicks) {
		if (be.phase == Phase.IDLE) {
			ItemStack stack = be.getInventory()
				.getItem(0);
			if (!stack.isEmpty())
				state.items.add(new GridItem(resolve(be, stack), 0, 0, 0));
			state.idle = true;
			return;
		}

		state.idle = false;
		GroupedItems items = be.groupedItems;
		float distance = .5f;

		if (be.phase == Phase.CRAFTING) {
			items = be.groupedItemsBeforeCraft;
			items.calcStats();
			float progress = Mth.clamp((2000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
			float earlyProgress = Mth.clamp(progress * 2, 0, 1);
			float lateProgress = Mth.clamp(progress * 2 - 1, 0, 1);

			state.craftingScale = 1 - lateProgress;
			state.centering = new Vec3(-items.minX + (-items.width + 1) / 2f,
				-items.minY + (-items.height + 1) / 2f, 0).scale(earlyProgress);
			distance += (-4 * (progress - .5f) * (progress - .5f) + 1) * .25f;
		} else {
			state.craftingScale = 1;
			state.centering = Vec3.ZERO;
		}

		state.spacing = distance;
		boolean onlyRenderFirst = be.phase == Phase.INSERTING || be.phase == Phase.CRAFTING && be.countDown < 1000;

		int offset = 0;
		if (be.phase == Phase.EXPORTING && be.getBlockState()
			.hasProperty(MechanicalCrafterBlock.POINTING)) {
			Pointing value = be.getBlockState()
				.getValue(MechanicalCrafterBlock.POINTING);
			offset = value == Pointing.UP ? -1 : value == Pointing.LEFT ? 2 : value == Pointing.RIGHT ? -2 : 1;
		}
		final int pointingOffset = offset;

		items.grid.forEach((pair, stack) -> {
			if (onlyRenderFirst && (pair.getLeft()
				.intValue() != 0
				|| pair.getRight()
					.intValue() != 0))
				return;
			int x = pair.getKey();
			int y = pair.getValue();
			state.items.add(new GridItem(resolve(be, stack), x, y,
				(x + y * 3 + pointingOffset * 9) / 1024f));
		});

		if (be.phase != Phase.CRAFTING)
			return;

		GroupedItems result = be.groupedItems;
		float progress = Mth.clamp((1000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
		float earlyProgress = Mth.clamp(progress * 2, 0, 1);
		float lateProgress = Mth.clamp(progress * 2 - 1, 0, 1);

		state.resultSpin = earlyProgress * 2 * 360;
		state.resultScale = (earlyProgress * 1.125f) * (1 + (1 - lateProgress) * .125f);

		result.grid.forEach((pair, stack) -> {
			if (pair.getLeft()
				.intValue() != 0
				|| pair.getRight()
					.intValue() != 0)
				return;
			state.craftResult.add(resolve(be, stack));
		});
	}

	private ItemStackRenderState resolve(MechanicalCrafterBlockEntity be, ItemStack stack) {
		ItemStackRenderState item = new ItemStackRenderState();
		itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
		return item;
	}

	protected void submitItems(CrafterRenderState state, PoseStack ms, SubmitNodeCollector queue) {
		if (state.idle) {
			for (GridItem item : state.items) {
				ms.pushPose();
				ms.translate(0, 0, -1 / 256f);
				ms.mulPose(Axis.YP.rotationDegrees(180));
				item.item()
					.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				ms.popPose();
			}
			return;
		}

		ms.pushPose();
		ms.scale(state.craftingScale, state.craftingScale, state.craftingScale);
		ms.translate(state.centering.x * .5f, state.centering.y * .5f, 0);

		for (GridItem item : state.items) {
			ms.pushPose();
			ms.translate(item.x() * state.spacing, item.y() * state.spacing, 0);
			TransformStack.of(ms)
				.rotateYDegrees(180)
				.translate(0, 0, item.zNudge());
			item.item()
				.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		ms.popPose();

		if (state.craftResult.isEmpty())
			return;

		ms.mulPose(Axis.ZP.rotationDegrees(state.resultSpin));
		ms.scale(state.resultScale, state.resultScale, state.resultScale);
		for (ItemStackRenderState result : state.craftResult) {
			ms.pushPose();
			ms.mulPose(Axis.YP.rotationDegrees(180));
			result.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	public void extractStatic(MechanicalCrafterBlockEntity be, CrafterRenderState state) {
		BlockState blockState = be.getBlockState();

		if (!VisualizationManager.supportsVisualization(be.getLevel())) {
			SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
			standardKineticRotationTransform(superBuffer, be, state.lightCoords);
			TransformStack.of(superBuffer.getTransforms())
				.rotateCentered((float) (blockState.getValue(HORIZONTAL_FACING)
					.getAxis() != Direction.Axis.X ? 0 : Math.PI / 2), Direction.UP)
				.rotateCentered((float) (Math.PI / 2), Direction.EAST);
			state.parts.add(superBuffer.extractRenderState());
		}

		Direction targetDirection = MechanicalCrafterBlock.getTargetDirection(blockState);
		BlockPos pos = be.getBlockPos();

		if ((be.covered || be.phase != Phase.IDLE) && be.phase != Phase.CRAFTING && be.phase != Phase.INSERTING)
			state.parts.add(renderAndTransform(AllPartialModels.MECHANICAL_CRAFTER_LID, blockState)
				.light(state.lightCoords)
				.extractRenderState());

		if (MechanicalCrafterBlock.isValidTarget(be.getLevel(), pos.relative(targetDirection), blockState)) {
			SuperByteBuffer beltBuffer = renderAndTransform(AllPartialModels.MECHANICAL_CRAFTER_BELT, blockState);
			SuperByteBuffer beltFrameBuffer =
				renderAndTransform(AllPartialModels.MECHANICAL_CRAFTER_BELT_FRAME, blockState);

			if (be.phase == Phase.EXPORTING) {
				int textureIndex = (int) ((be.getCountDownSpeed() / 128f * AnimationTickHolder.getTicks()));
				beltBuffer.shiftUVtoSheet(AllSpriteShifts.CRAFTER_THINGIES, (textureIndex % 4) / 4f, 0, 1);
			}

			state.parts.add(beltBuffer.light(state.lightCoords)
				.extractRenderState());
			state.parts.add(beltFrameBuffer.light(state.lightCoords)
				.extractRenderState());

		} else {
			state.parts.add(renderAndTransform(AllPartialModels.MECHANICAL_CRAFTER_ARROW, blockState)
				.light(state.lightCoords)
				.extractRenderState());
		}
	}

	private SuperByteBuffer renderAndTransform(PartialModel renderBlock, BlockState crafterState) {
		SuperByteBuffer buffer = CachedBufferer.partial(renderBlock, crafterState);
		float xRot = crafterState.getValue(MechanicalCrafterBlock.POINTING)
			.getXRotation();
		float yRot = AngleHelper.horizontalAngle(crafterState.getValue(HORIZONTAL_FACING));
		TransformStack.of(buffer.getTransforms())
			.rotateCentered((float) ((yRot + 90) / 180 * Math.PI), Direction.UP)
			.rotateCentered((float) ((xRot) / 180 * Math.PI), Direction.EAST);
		return buffer;
	}

}
