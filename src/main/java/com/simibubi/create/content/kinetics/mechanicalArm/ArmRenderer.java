package com.simibubi.create.content.kinetics.mechanicalArm;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.logistics.depot.DepotRenderer.ItemState;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ArmRenderer extends KineticBlockEntityRenderer<ArmBlockEntity, ArmRenderer.ArmRenderState> {

	public static class ArmRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> arm = new ArrayList<>(6);
		public @Nullable ItemState heldItem;
		public boolean goggles;
		public boolean isBlockItem;
		/** The arm's own transform, resolved during extraction and reapplied when the item is drawn. */
		public @Nullable PoseStack itemTransform;
	}

	public ArmRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ArmRenderState createRenderState() {
		return new ArmRenderState();
	}

	@Override
	protected void extractSafe(ArmBlockEntity be, ArmRenderState state, float pt, Vec3 cameraPosition) {
		super.extractSafe(be, state, pt, cameraPosition);
		state.arm.clear();
		state.heldItem = null;
		state.itemTransform = null;

		ItemStack item = be.heldItem;
		boolean hasItem = !item.isEmpty();
		boolean usingFlywheel = VisualizationManager.supportsVisualization(be.getLevel());

		if (usingFlywheel && !hasItem)
			return;

		state.goggles = be.goggles;
		BlockState blockState = be.getBlockState();

		PoseStack msLocal = new PoseStack();
		var msr = TransformStack.of(msLocal);

		float baseAngle;
		float lowerArmAngle;
		float upperArmAngle;
		float headAngle;
		int color;
		boolean inverted = blockState.getValue(ArmBlock.CEILING);

		boolean rave = be.phase == Phase.DANCING && be.getSpeed() != 0;
		if (rave) {
			float renderTick = AnimationTickHolder.getRenderTime(be.getLevel()) + (be.hashCode() % 64);
			baseAngle = (renderTick * 10) % 360;
			lowerArmAngle = Mth.lerp((Mth.sin(renderTick / 4) + 1) / 2, -45, 15);
			upperArmAngle = Mth.lerp((Mth.sin(renderTick / 8) + 1) / 4, -45, 95);
			headAngle = -lowerArmAngle;
			color = Color.rainbowColor(AnimationTickHolder.getTicks() * 100)
				.getRGB();
		} else {
			baseAngle = be.baseAngle.getValue(pt);
			lowerArmAngle = be.lowerArmAngle.getValue(pt) - 135;
			upperArmAngle = be.upperArmAngle.getValue(pt) - 90;
			headAngle = be.headAngle.getValue(pt);
			color = 0xFFFFFF;
		}

		msr.center();

		if (inverted)
			msr.rotateXDegrees(180);

		if (usingFlywheel)
			doItemTransforms(msr, baseAngle, lowerArmAngle, upperArmAngle, headAngle);
		else
			extractArm(state, msLocal, msr, blockState, color, baseAngle, lowerArmAngle, upperArmAngle, headAngle,
				be.goggles, inverted && be.goggles, hasItem, item, state.lightCoords);

		if (!hasItem)
			return;

		ItemState heldItem = ItemState.create(itemModelResolver, item, be.getLevel());
		state.heldItem = heldItem;
		state.isBlockItem = item.getItem() instanceof BlockItem && heldItem.blockItem();

		float itemScale = state.isBlockItem ? .5f : .625f;
		msr.rotateXDegrees(90);
		msLocal.translate(0, state.isBlockItem ? -9 / 16f : -10 / 16f, 0);
		msLocal.scale(itemScale, itemScale, itemScale);
		state.itemTransform = msLocal;
	}

	@Override
	protected void submitSafe(ArmRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		for (SuperByteBufferRenderState part : state.arm)
			part.submit(ms, state.goggles ? RenderTypes.cutoutMovingBlock() : RenderTypes.solidMovingBlock(), queue);

		if (state.heldItem == null || state.itemTransform == null)
			return;

		ms.pushPose();
		ms.last()
			.pose()
			.mul(state.itemTransform.last()
				.pose());
		state.heldItem.item()
			.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	/**
	 * Each joint is drawn with the transform accumulated so far, so every part is baked into its own
	 * state as the arm is walked rather than sharing one buffer.
	 */
	private void extractArm(ArmRenderState state, PoseStack msLocal, TransformStack msr, BlockState blockState,
		int color, float baseAngle, float lowerArmAngle, float upperArmAngle, float headAngle, boolean goggles,
		boolean inverted, boolean hasItem, ItemStack item, int light) {
		boolean isBlockItem = hasItem && item.getItem() instanceof BlockItem;

		transformBase(msr, baseAngle);
		state.arm.add(baked(AllPartialModels.ARM_BASE, blockState, light, null, msLocal));

		transformLowerArm(msr, lowerArmAngle);
		state.arm.add(baked(AllPartialModels.ARM_LOWER_BODY, blockState, light, color, msLocal));

		transformUpperArm(msr, upperArmAngle);
		state.arm.add(baked(AllPartialModels.ARM_UPPER_BODY, blockState, light, color, msLocal));

		transformHead(msr, headAngle);

		if (inverted)
			msr.rotateZDegrees(180);
		state.arm.add(baked(goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE,
			blockState, light, null, msLocal));

		if (inverted)
			msr.rotateZDegrees(180);

		for (int flip : Iterate.positiveAndNegative) {
			msLocal.pushPose();
			transformClawHalf(msr, hasItem, isBlockItem, flip);
			state.arm.add(baked(flip > 0 ? AllPartialModels.ARM_CLAW_GRIP_LOWER : AllPartialModels.ARM_CLAW_GRIP_UPPER,
				blockState, light, null, msLocal));
			msLocal.popPose();
		}
	}

	private static SuperByteBufferRenderState baked(PartialModel model,
		BlockState blockState, int light, @Nullable Integer color, PoseStack msLocal) {
		SuperByteBuffer buffer = CachedBufferer.partial(model, blockState)
			.light(light);
		if (color != null)
			buffer.color(color);
		buffer.transform(msLocal);
		return buffer.extractRenderState();
	}

	private void doItemTransforms(TransformStack msr, float baseAngle, float lowerArmAngle, float upperArmAngle,
		float headAngle) {

		transformBase(msr, baseAngle);
		transformLowerArm(msr, lowerArmAngle);
		transformUpperArm(msr, upperArmAngle);
		transformHead(msr, headAngle);
	}

	public static void transformClawHalf(TransformStack msr, boolean hasItem, boolean isBlockItem, int flip) {
		msr.translate(0, -flip * (hasItem ? isBlockItem ? 3 / 16f : 5 / 64f : 1 / 16f), -6 / 16d);
	}

	public static void transformHead(TransformStack msr, float headAngle) {
		msr.translate(0, 0, -15 / 16d);
		msr.rotateXDegrees(headAngle - 45f);
	}

	public static void transformUpperArm(TransformStack msr, float upperArmAngle) {
		msr.translate(0, 0, -14 / 16d);
		msr.rotateXDegrees(upperArmAngle - 90);
	}

	public static void transformLowerArm(TransformStack msr, float lowerArmAngle) {
		msr.translate(0, 2 / 16d, 0);
		msr.rotateXDegrees(lowerArmAngle + 135);
	}

	public static void transformBase(TransformStack msr, float baseAngle) {
		msr.translate(0, 4 / 16d, 0);
		msr.rotateYDegrees(baseAngle);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected SuperByteBuffer getRotatedModel(ArmBlockEntity be, BlockState state) {
		return CachedBufferer.partial(AllPartialModels.ARM_COG, state);
	}

}
