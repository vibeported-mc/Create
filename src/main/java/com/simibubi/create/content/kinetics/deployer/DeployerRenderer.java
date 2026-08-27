package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer.FilterRenderState;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity.Mode;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DeployerRenderer
	extends SafeBlockEntityRenderer<DeployerBlockEntity, DeployerRenderer.DeployerRenderState> {

	public static class DeployerRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(3);
		public @Nullable HeldItem heldItem;
		public @Nullable FilterRenderState filter;
	}

	protected final ItemModelResolver itemModelResolver;

	public DeployerRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public DeployerRenderState createRenderState() {
		return new DeployerRenderState();
	}

	@Override
	protected void extractSafe(DeployerBlockEntity be, DeployerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.parts.clear();
		state.heldItem = null;
		state.filter = FilteringRenderer.getFilterRenderState(be, itemModelResolver, cameraPosition);

		extractItem(be, state, partialTicks);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		extractComponents(be, state, partialTicks);
	}

	@Override
	protected void submitSafe(DeployerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.heldItem != null)
			state.heldItem.submit(ms, queue, state.lightCoords);

		if (state.filter != null)
			state.filter.submit(state.blockState, queue, ms, state.lightCoords);

		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	protected void extractItem(DeployerBlockEntity be, DeployerRenderState state, float partialTicks) {
		if (be.heldItem.isEmpty())
			return;

		BlockState deployerState = be.getBlockState();
		Vec3 offset = getHandOffset(be, partialTicks, deployerState).add(VecHelper.getCenterOf(BlockPos.ZERO));

		Direction facing = deployerState.getValue(FACING);
		boolean punching = be.mode == Mode.PUNCH;

		float yRot = AngleHelper.horizontalAngle(facing) + 180;
		float xRot = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
		boolean displayMode = facing == Direction.UP && be.getSpeed() == 0 && !punching;

		ItemStackRenderState item = new ItemStackRenderState();
		ItemDisplayContext transform;
		boolean isBlockItem;

		if (displayMode) {
			transform = ItemDisplayContext.GROUND;
		} else {
			transform = punching ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.FIXED;
		}
		item.displayContext = transform;
		itemModelResolver.appendItemLayers(item, be.heldItem, transform, be.getLevel(), null, 0);
		isBlockItem = be.heldItem.getItem() instanceof BlockItem && item.usesBlockLight();

		state.heldItem = new HeldItem(item, offset, yRot, xRot, displayMode, punching, isBlockItem,
			AnimationTickHolder.getRenderTime(be.getLevel()));
	}

	protected void extractComponents(DeployerBlockEntity be, DeployerRenderState state, float partialTicks) {
		state.parts.add(KineticBlockEntityRenderer.extractRotatingKineticBlock(be, getRenderedBlockState(be),
			state.lightCoords));

		BlockState blockState = be.getBlockState();
		Vec3 offset = getHandOffset(be, partialTicks, blockState);

		SuperByteBuffer pole = CachedBufferer.partial(AllPartialModels.DEPLOYER_POLE, blockState);
		SuperByteBuffer hand = CachedBufferer.partial(be.getHandPose(), blockState);

		TransformStack.of(pole.getTransforms())
			.translate(offset.x, offset.y, offset.z);
		TransformStack.of(hand.getTransforms())
			.translate(offset.x, offset.y, offset.z);

		state.parts.add(transform(pole, blockState, true).light(state.lightCoords)
			.extractRenderState());
		state.parts.add(transform(hand, blockState, false).light(state.lightCoords)
			.extractRenderState());
	}

	/**
	 * The deployer's held item hangs off the pole, so its placement follows the hand offset resolved
	 * during extraction. Display mode spins it on the spot using the animation clock.
	 */
	public record HeldItem(ItemStackRenderState item, Vec3 offset, float yRot, float xRot, boolean displayMode,
		boolean punching, boolean isBlockItem, float renderTime) {

		public void submit(PoseStack ms, SubmitNodeCollector queue, int light) {
			ms.pushPose();
			ms.translate(offset.x, offset.y, offset.z);

			ms.mulPose(Axis.YP.rotationDegrees(yRot));
			if (!displayMode) {
				ms.mulPose(Axis.XP.rotationDegrees(xRot));
				ms.translate(0, 0, -11 / 16f);
			}

			if (punching)
				ms.translate(0, 1 / 8f, -1 / 16f);

			if (displayMode) {
				float scale = isBlockItem ? 1.25f : 1;
				ms.translate(0, isBlockItem ? 9 / 16f : 11 / 16f, 0);
				ms.scale(scale, scale, scale);
				ms.mulPose(Axis.YP.rotationDegrees(renderTime));
			} else {
				float scale = punching ? .75f : isBlockItem ? .75f - 1 / 64f : .5f;
				ms.scale(scale, scale, scale);
			}

			item.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	protected Vec3 getHandOffset(DeployerBlockEntity be, float partialTicks, BlockState blockState) {
		float distance = be.getHandOffset(partialTicks);
		return Vec3.atLowerCornerOf(blockState.getValue(FACING).getUnitVec3i()).scale(distance);
	}

	protected BlockState getRenderedBlockState(KineticBlockEntity be) {
		return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(be));
	}

	private static SuperByteBuffer transform(SuperByteBuffer buffer, BlockState deployerState, boolean axisDirectionMatters) {
		Direction facing = deployerState.getValue(FACING);

		float yRot = AngleHelper.horizontalAngle(facing);
		float xRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
		float zRot =
			axisDirectionMatters && (deployerState.getValue(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z) ? 90
				: 0;

		TransformStack.of(buffer.getTransforms())
			.rotateCentered((float) ((yRot) / 180 * Math.PI), Direction.UP)
			.rotateCentered((float) ((xRot) / 180 * Math.PI), Direction.EAST)
			.rotateCentered((float) ((zRot) / 180 * Math.PI), Direction.SOUTH);
		return buffer;
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		BlockState blockState = context.state;
		Mode mode = NBTHelper.readEnum(context.blockEntityData, "Mode", Mode.class);
		PartialModel handPose = getHandPose(mode);

		float speed = (float) context.getAnimationSpeed();
		if (context.contraption.stalled)
			speed = 0;

		SuperByteBuffer shaft = CachedBuffers.block(AllBlocks.SHAFT.getDefaultState());
		SuperByteBuffer pole = CachedBufferer.partial(AllPartialModels.DEPLOYER_POLE, blockState);
		SuperByteBuffer hand = CachedBufferer.partial(handPose, blockState);

		double factor;
		if (context.contraption.stalled || context.position == null || context.data.contains("StationaryTimer")) {
			factor = Mth.sin(AnimationTickHolder.getRenderTime() * .5f) * .25f + .25f;
		} else {
			Vec3 center = VecHelper.getCenterOf(BlockPos.containing(context.position));
			double distance = context.position.distanceTo(center);
			double nextDistance = context.position.add(context.motion)
				.distanceTo(center);
			factor = .5f - Mth.clamp(Mth.lerp(AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0, 1);
		}

		Vec3 offset = Vec3.atLowerCornerOf(blockState.getValue(FACING)
			.getUnitVec3i()).scale(factor);

		PoseStack m = matrices.getModel();
		m.pushPose();

		m.pushPose();
		Direction.Axis axis = Direction.Axis.Y;
		if (context.state.getBlock() instanceof IRotate def) {
			axis = def.getRotationAxis(context.state);
		}

		float time = AnimationTickHolder.getRenderTime(context.world) / 20;
		float angle = (time * speed) % 360;

		TransformStack.of(m)
			.center()
			.rotateYDegrees(axis == Direction.Axis.Z ? 90 : 0)
			.rotateZDegrees(axis.isHorizontal() ? 90 : 0)
			.uncenter();
		shaft.transform(m);
		TransformStack.of(shaft.getTransforms())
			.rotateCentered(angle, Direction.get(AxisDirection.POSITIVE, Direction.Axis.Y));
		m.popPose();

		if (!context.disabled)
			m.translate(offset.x, offset.y, offset.z);
		pole.transform(m);
		hand.transform(m);

		transform(pole, blockState, true);
		transform(hand, blockState, false);

		int contraptionLight = LightCoordsUtil.getLightCoords(renderWorld, context.localPos);
		for (SuperByteBuffer buf : new SuperByteBuffer[] { shaft, pole, hand }) {
			buf.light(contraptionLight)
				.useLevelLight(context.world, matrices.getWorld());
			out.add(ActorGeometry.of(matrices.getViewProjection(), buf, RenderTypes.solidMovingBlock()));
		}

		m.popPose();
	}

	static PartialModel getHandPose(DeployerBlockEntity.Mode mode) {
		return mode == DeployerBlockEntity.Mode.PUNCH ? AllPartialModels.DEPLOYER_HAND_PUNCHING : AllPartialModels.DEPLOYER_HAND_POINTING;
	}

}
