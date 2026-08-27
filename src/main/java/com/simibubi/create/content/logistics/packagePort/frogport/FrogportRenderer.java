package com.simibubi.create.content.logistics.packagePort.frogport;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FrogportRenderer
	extends SmartBlockEntityRenderer<FrogportBlockEntity, FrogportRenderer.FrogportRenderState> {

	public static class FrogportRenderState extends SmartRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(5);
	}

	public FrogportRenderer(Context context) {
		super(context);
	}

	@Override
	public FrogportRenderState createRenderState() {
		return new FrogportRenderState();
	}

	@Override
	protected void extractSafe(FrogportBlockEntity be, FrogportRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.parts.clear();

		state.nameplate = null;
		if (be.addressFilter != null && !be.addressFilter.isBlank())
			state.nameplate = extractNameplateOnHover(be, Component.literal(be.addressFilter), 1, cameraPosition,
				state.lightCoords);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		float yaw = be.getYaw();

		float headPitch = 80;
		float tonguePitch = 0;
		float tongueLength = 0;
		float headPitchModifier = 1;

		boolean hasTarget = be.target != null;
		boolean animating = be.isAnimationInProgress();
		boolean depositing = be.currentlyDepositing;

		Vec3 diff = Vec3.ZERO;

		if (hasTarget) {
			diff = be.target.getExactTargetLocation(be, be.getLevel(), be.getBlockPos())
				.subtract(0, animating && depositing ? 0 : 0.75, 0)
				.subtract(Vec3.atCenterOf(be.getBlockPos()));
			tonguePitch = (float) Mth.atan2(diff.y, diff.multiply(1, 0, 1)
				.length() + (3 / 16f)) * Mth.RAD_TO_DEG;
			tongueLength = Math.max((float) diff.length(), 1);
			headPitch = Mth.clamp(tonguePitch * 2, 60, 100);
		}

		if (animating) {
			float progress = be.animationProgress.getValue(partialTicks);
			float scale = 1;
			float itemDistance = 0;

			if (depositing) {
				double modifier = Math.max(0, 1 - Math.pow((progress - 0.25) * 4 - 1, 4));
				itemDistance =
					(float) Math.max(tongueLength * Math.min(1, (progress - 0.25) * 3), tongueLength * modifier);
				tongueLength *= Math.max(0, 1 - Math.pow((progress * 1.25 - 0.25) * 4 - 1, 4));
				headPitchModifier = (float) Math.max(0, 1 - Math.pow((progress * 1.25) * 2 - 1, 4));
				scale = 0.25f + progress * 3 / 4;

			} else {
				tongueLength *= Math.pow(Math.max(0, 1 - progress * 1.25), 5);
				headPitchModifier = 1 - (float) Math.min(1, Math.max(0, (Math.pow(progress * 1.5, 2) - 0.5) * 2));
				scale = (float) Math.max(0.5, 1 - progress * 1.25);
				itemDistance = tongueLength;
			}

			extractPackage(be, state, diff, scale, itemDistance);

		} else {
			tongueLength = 0;
			float anticipation = be.anticipationProgress.getValue(partialTicks);
			headPitchModifier =
				anticipation > 0 ? (float) Math.max(0, 1 - Math.pow((anticipation * 1.25) * 2 - 1, 4)) : 0;
		}

		headPitch *= headPitchModifier;

		headPitch = Math.max(headPitch, be.manualOpenAnimationProgress.getValue(partialTicks) * 60);
		tongueLength = Math.max(tongueLength, be.manualOpenAnimationProgress.getValue(partialTicks) * 0.25f);

		SuperByteBuffer body = CachedBuffers.partial(AllPartialModels.FROGPORT_BODY, be.getBlockState());
		TransformStack.of(body.getTransforms())
			.center()
			.rotateYDegrees(yaw)
			.uncenter();
		state.parts.add(body.light(state.lightCoords)
			.extractRenderState());

		SuperByteBuffer head = CachedBuffers.partial(
			be.goggles ? AllPartialModels.FROGPORT_HEAD_GOGGLES : AllPartialModels.FROGPORT_HEAD, be.getBlockState());
		TransformStack.of(head.getTransforms())
			.center()
			.rotateYDegrees(yaw)
			.uncenter()
			.translate(8 / 16f, 10 / 16f, 11 / 16f)
			.rotateXDegrees(headPitch)
			.translateBack(8 / 16f, 10 / 16f, 11 / 16f);
		state.parts.add(head.light(state.lightCoords)
			.extractRenderState());

		SuperByteBuffer tongue = CachedBuffers.partial(AllPartialModels.FROGPORT_TONGUE, be.getBlockState());
		TransformStack.of(tongue.getTransforms())
			.center()
			.rotateYDegrees(yaw)
			.uncenter()
			.translate(8 / 16f, 10 / 16f, 11 / 16f)
			.rotateXDegrees(tonguePitch)
			.scale(1f, 1f, tongueLength / (7 / 16f))
			.translateBack(8 / 16f, 10 / 16f, 11 / 16f);
		state.parts.add(tongue.light(state.lightCoords)
			.extractRenderState());
	}

	@Override
	protected void submitSafe(FrogportRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	private void extractPackage(FrogportBlockEntity be, FrogportRenderState state, Vec3 diff, float scale,
		float itemDistance) {
		if (be.animatedPackage == null)
			return;
		if (scale < 0.45)
			return;
		Identifier key = BuiltInRegistries.ITEM.getKey(be.animatedPackage.getItem());
		if (key == BuiltInRegistries.ITEM.getDefaultKey())
			return;
		SuperByteBuffer rigBuffer = CachedBuffers.partial(AllPartialModels.PACKAGE_RIGGING.get(key),
			be.getBlockState());
		SuperByteBuffer boxBuffer = CachedBuffers.partial(AllPartialModels.PACKAGES.get(key), be.getBlockState());

		boolean animating = be.isAnimationInProgress();
		boolean depositing = be.currentlyDepositing;

		for (SuperByteBuffer buf : new SuperByteBuffer[] { boxBuffer, rigBuffer }) {
			TransformStack.of(buf.getTransforms())
				.translate(0, 3 / 16f, 0)
				.translate(diff.normalize()
					.scale(itemDistance)
					.subtract(0, animating && depositing ? 0.75 : 0, 0))
				.center()
				.scale(scale)
				.uncenter();
			state.parts.add(buf.light(state.lightCoords)
				.extractRenderState());
			if (!be.currentlyDepositing)
				break;
		}
	}

}
