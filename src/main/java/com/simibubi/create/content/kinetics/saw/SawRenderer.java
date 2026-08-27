package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.util.LightCoordsUtil;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.FilterRenderState;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SawRenderer extends SafeBlockEntityRenderer<SawBlockEntity, SawRenderer.SawRenderState> {

	public static class SawRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState blade;
		public @Nullable SuperByteBufferRenderState shaft;
		public @Nullable FilterRenderState filter;
		public final List<SawItem> items = new ArrayList<>();
		public boolean alongZ;
		public float offset;
		public int outputs;
	}

	/** One item on the saw; `renderedIndex` is its position among the non-empty slots. */
	public record SawItem(ItemStackRenderState item, int slot, int renderedIndex, boolean blockItem, boolean box) {
	}

	protected final ItemModelResolver itemModelResolver;

	public SawRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public SawRenderState createRenderState() {
		return new SawRenderState();
	}

	@Override
	protected void extractSafe(SawBlockEntity be, SawRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.blade = null;
		state.shaft = null;
		state.items.clear();

		extractBlade(be, state);
		extractItems(be, state, partialTicks);
		state.filter = FilteringRenderer.getFilterRenderState(be, itemModelResolver, cameraPosition);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		state.shaft = KineticBlockEntityRenderer
			.standardKineticRotationTransform(getRotatedModel(be), be, state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(SawRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.blade != null)
			state.blade.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		if (state.shaft != null)
			state.shaft.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.filter != null)
			state.filter.submit(state.blockState, queue, ms, state.lightCoords);

		submitItems(state, ms, queue);
	}

	protected void extractBlade(SawBlockEntity be, SawRenderState state) {
		BlockState blockState = be.getBlockState();
		PartialModel partial;
		float speed = be.getSpeed();
		boolean rotate = false;

		if (SawBlock.isHorizontal(blockState)) {
			if (speed > 0) {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE;
			} else if (speed < 0) {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_REVERSED;
			} else {
				partial = AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE;
			}
		} else {
			if (be.getSpeed() > 0) {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE;
			} else if (speed < 0) {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_REVERSED;
			} else {
				partial = AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE;
			}

			if (blockState.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE))
				rotate = true;
		}

		SuperByteBuffer superBuffer = CachedBufferer.partialFacing(partial, blockState);
		if (rotate)
			TransformStack.of(superBuffer.getTransforms())
				.rotateCentered(AngleHelper.rad(90), Direction.UP);

		state.blade = superBuffer.color(0xFFFFFF)
			.light(state.lightCoords)
			.extractRenderState();
	}

	protected void extractItems(SawBlockEntity be, SawRenderState state, float partialTicks) {
		if (be.getBlockState()
			.getValue(SawBlock.FACING) != Direction.UP)
			return;
		if (be.inventory.isEmpty())
			return;

		boolean alongZ = !be.getBlockState()
			.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE);

		float duration = be.inventory.recipeDuration;
		boolean moving = duration != 0;
		float offset = moving ? (float) (be.inventory.remainingTime) / duration : 0;
		float processingSpeed = Mth.clamp(Math.abs(be.getSpeed()) / 32, 1, 128);
		if (moving) {
			offset = Mth.clamp(offset + ((-partialTicks + .5f) * processingSpeed) / duration, 0.125f, 1f);
			if (!be.inventory.appliedRecipe)
				offset += 1;
			offset /= 2;
		}

		if (be.getSpeed() == 0)
			offset = .5f;
		if (be.getSpeed() < 0 ^ alongZ)
			offset = 1 - offset;

		int outputs = 0;
		for (int i = 1; i < be.inventory.size(); i++)
			if (!be.inventory.getResource(i)
				.isEmpty())
				outputs++;

		state.alongZ = alongZ;
		state.offset = offset;
		state.outputs = outputs;

		int renderedI = 0;
		for (int i = 0; i < be.inventory.size(); i++) {
			ItemStack stack = ItemHandlerHelpers.getStackInSlot(be.inventory, i);
			if (stack.isEmpty())
				continue;

			ItemStackRenderState item = new ItemStackRenderState();
			item.displayContext = ItemDisplayContext.FIXED;
			itemModelResolver.appendItemLayers(item, stack, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
			state.items.add(new SawItem(item, i, renderedI, item.usesBlockLight(), PackageItem.isPackage(stack)));
			renderedI++;
		}
	}

	protected void submitItems(SawRenderState state, PoseStack ms, SubmitNodeCollector queue) {
		if (state.items.isEmpty())
			return;

		ms.pushPose();
		if (state.alongZ)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		ms.translate(state.outputs <= 1 ? .5 : .25, 0, state.offset);
		ms.translate(state.alongZ ? -1 : 0, 0, 0);

		for (SawItem sawItem : state.items) {
			ms.pushPose();
			ms.translate(0, sawItem.blockItem() ? .925f : 13f / 16f, 0);

			if (sawItem.slot() > 0 && state.outputs > 1) {
				ms.translate((0.5 / (state.outputs - 1)) * sawItem.renderedIndex(), 0, 0);
				TransformStack.of(ms)
					.nudge(sawItem.slot() * 133);
			}

			if (sawItem.box()) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(.5f, .5f, .5f);
				ms.mulPose(Axis.XP.rotationDegrees(90));
			}

			sawItem.item()
				.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		ms.popPose();
	}

	protected SuperByteBuffer getRotatedModel(KineticBlockEntity be) {
		BlockState state = be.getBlockState();
		if (state.getValue(FACING)
			.getAxis()
			.isHorizontal())
			return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF,
				state.rotate(be.getLevel(), be.getBlockPos(), Rotation.CLOCKWISE_180));
		return CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK, getRenderedBlockState(be));
	}

	protected BlockState getRenderedBlockState(KineticBlockEntity be) {
		return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be));
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		BlockState state = context.state;
		Direction facing = state.getValue(SawBlock.FACING);

		Vec3 facingVec = Vec3.atLowerCornerOf(context.state.getValue(SawBlock.FACING)
			.getUnitVec3i());
		facingVec = context.rotation.apply(facingVec);

		Direction closestToFacing = Direction.getNearest(facingVec.x, facingVec.y, facingVec.z);

		boolean horizontal = closestToFacing.getAxis()
			.isHorizontal();
		boolean backwards = VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite());
		boolean moving = context.getAnimationSpeed() != 0;
		boolean shouldAnimate =
			(context.contraption.stalled && horizontal) || (!context.contraption.stalled && !backwards && moving);

		SuperByteBuffer superBuffer;
		if (SawBlock.isHorizontal(state)) {
			if (shouldAnimate)
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE, state);
			else
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE, state);
		} else {
			if (shouldAnimate)
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE, state);
			else
				superBuffer = CachedBufferer.partial(AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE, state);
		}

		superBuffer.transform(matrices.getModel())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing));

		if (!SawBlock.isHorizontal(state)) {
			superBuffer.rotateZDegrees(state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? 90 : 0);
		}

		superBuffer.uncenter()
			.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld());
		out.add(ActorGeometry.of(matrices.getViewProjection(), superBuffer, RenderTypes.cutoutMovingBlock()));
	}

}
