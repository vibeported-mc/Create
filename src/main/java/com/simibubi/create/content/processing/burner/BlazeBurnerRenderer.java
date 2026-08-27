package com.simibubi.create.content.processing.burner;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlazeBurnerRenderer
	extends SafeBlockEntityRenderer<BlazeBurnerBlockEntity, BlazeBurnerRenderer.BlazeBurnerRenderState> {

	public static class BlazeBurnerRenderState extends SafeRenderState {
		public HeatLevel heatLevel = HeatLevel.NONE;
		public @Nullable Level level;
		public @Nullable BlockState state;
		public float animation;
		public float horizontalAngle;
		public boolean canDrawFlame;
		public boolean drawGoggles;
		public @Nullable PartialModel drawHat;
		public int hashCode;
	}

	public BlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public BlazeBurnerRenderState createRenderState() {
		return new BlazeBurnerRenderState();
	}

	@Override
	protected void extractSafe(BlazeBurnerBlockEntity be, BlazeBurnerRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.heatLevel = be.getHeatLevelFromBlock();
		if (state.heatLevel == HeatLevel.NONE) {
			state.skip = true;
			return;
		}

		state.level = be.getLevel();
		state.state = be.getBlockState();
		state.animation = be.headAnimation.getValue(partialTicks) * .175f;
		state.horizontalAngle = AngleHelper.rad(be.headAngle.getValue(partialTicks));
		state.canDrawFlame = state.heatLevel.isAtLeast(HeatLevel.FADING);
		state.drawGoggles = be.goggles;
		state.drawHat = be.hat ? AllPartialModels.TRAIN_HAT
			: be.stockKeeper ? AllPartialModels.LOGISTICS_HAT : null;
		state.hashCode = be.hashCode();
	}

	/**
	 * The shared drawing routine reads nothing but its arguments and the animation clock, so it can be
	 * called straight from submission with the values captured during extraction.
	 */
	@Override
	protected void submitSafe(BlazeBurnerRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.level == null || state.state == null)
			return;
		submitShared(ms, null, queue, state.level, state.state, state.heatLevel, state.animation,
			state.horizontalAngle, state.canDrawFlame, state.drawGoggles, state.drawHat, state.hashCode);
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out, LerpedFloat headAngle, boolean conductor) {
		BlockState state = context.state;
		HeatLevel heatLevel = BlazeBurnerBlock.getHeatLevelOf(state);
		if (heatLevel == HeatLevel.NONE)
			return;

		if (!heatLevel.isAtLeast(HeatLevel.FADING))
			heatLevel = HeatLevel.FADING;

		HeatLevel drawnHeat = heatLevel;
		Level level = context.world;
		float horizontalAngle = AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level)));
		boolean drawGoggles = context.blockEntityData.contains("Goggles");
		boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");
		int hashCode = context.hashCode();

		// Everything the shared body reads is a plain value by this point - the models it builds are
		// client-side cache lookups - so it can run during submission like the block entity's does.
		PoseStack modelTransform = new PoseStack();
		modelTransform.last()
			.set(matrices.getModel()
				.last());
		PartialModel hat = drawHat ? AllPartialModels.TRAIN_HAT : null;
		out.add(ActorGeometry.at(matrices.getViewProjection(), (ms, queue) -> submitShared(ms, modelTransform, queue,
			level, state, drawnHeat, 0, horizontalAngle, false, drawGoggles, hat, hashCode)));
	}

	public static void submitShared(PoseStack ms, @Nullable PoseStack modelTransform, SubmitNodeCollector queue,
		Level level, BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
		boolean canDrawFlame, boolean drawGoggles, PartialModel drawHat, int hashCode) {

		boolean blockAbove = animation > 0.125f;
		float time = AnimationTickHolder.getRenderTime();
		float renderTick = time + (hashCode % 13) * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
		float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
		float headY = offset - (animation * .75f);

		ms.pushPose();

		var blazeModel = getBlazeModel(heatLevel, blockAbove);

		SuperByteBuffer blazeBuffer = CachedBufferer.partial(blazeModel, blockState);
		if (modelTransform != null)
			blazeBuffer.transform(modelTransform);
		TransformStack.of(blazeBuffer.getTransforms())
			.translate(0, headY, 0);
		submit(blazeBuffer, horizontalAngle, ms, queue, RenderTypes.solidMovingBlock());

		if (drawGoggles) {
			PartialModel gogglesModel = blazeModel == AllPartialModels.BLAZE_INERT
					? AllPartialModels.BLAZE_GOGGLES_SMALL : AllPartialModels.BLAZE_GOGGLES;

			SuperByteBuffer gogglesBuffer = CachedBufferer.partial(gogglesModel, blockState);
			if (modelTransform != null)
				gogglesBuffer.transform(modelTransform);
			TransformStack.of(gogglesBuffer.getTransforms())
				.translate(0, headY + 8 / 16f, 0);
			submit(gogglesBuffer, horizontalAngle, ms, queue, RenderTypes.solidMovingBlock());
		}

		if (drawHat != null) {
			SuperByteBuffer hatBuffer = CachedBufferer.partial(drawHat, blockState);
			if (modelTransform != null)
				hatBuffer.transform(modelTransform);
			var hatTr = TransformStack.of(hatBuffer.getTransforms());
			hatTr.translate(0, headY, 0);
			if (blazeModel == AllPartialModels.BLAZE_INERT) {
				hatTr.translateY(0.5f)
						.center()
						.scale(0.75f)
						.uncenter();
			} else {
				hatTr.translateY(0.75f);
			}
			hatTr.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
					.translate(0.5f, 0, 0.5f);
			hatBuffer.light(LightCoordsUtil.FULL_BRIGHT)
					.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		}

		if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			PartialModel rodsModel = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS
					: AllPartialModels.BLAZE_BURNER_RODS;
			PartialModel rodsModel2 = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
					: AllPartialModels.BLAZE_BURNER_RODS_2;

			SuperByteBuffer rodsBuffer = CachedBufferer.partial(rodsModel, blockState);
			if (modelTransform != null)
				rodsBuffer.transform(modelTransform);
			TransformStack.of(rodsBuffer.getTransforms())
					.translate(0, offset1 + animation + .125f, 0);
			rodsBuffer.light(LightCoordsUtil.FULL_BRIGHT)
					.submit(ms, RenderTypes.solidMovingBlock(), queue);

			SuperByteBuffer rodsBuffer2 = CachedBufferer.partial(rodsModel2, blockState);
			if (modelTransform != null)
				rodsBuffer2.transform(modelTransform);
			TransformStack.of(rodsBuffer2.getTransforms())
					.translate(0, offset2 + animation - 3 / 16f, 0);
			rodsBuffer2.light(LightCoordsUtil.FULL_BRIGHT)
					.submit(ms, RenderTypes.solidMovingBlock(), queue);
		}

		if (canDrawFlame && blockAbove) {
			SpriteShiftEntry spriteShift =
					heatLevel == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME : AllSpriteShifts.BURNER_FLAME;

			float spriteWidth = spriteShift.getTarget()
					.getU1()
					- spriteShift.getTarget()
					.getU0();

			float spriteHeight = spriteShift.getTarget()
					.getV1()
					- spriteShift.getTarget()
					.getV0();

			float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

			double vScroll = speed * time;
			vScroll = vScroll - Math.floor(vScroll);
			vScroll = vScroll * spriteHeight / 2;

			double uScroll = speed * time / 2;
			uScroll = uScroll - Math.floor(uScroll);
			uScroll = uScroll * spriteWidth / 2;

			SuperByteBuffer flameBuffer = CachedBufferer.partial(AllPartialModels.BLAZE_BURNER_FLAME, blockState);
			if (modelTransform != null)
				flameBuffer.transform(modelTransform);
			flameBuffer.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll);

			submit(flameBuffer, horizontalAngle, ms, queue, RenderTypes.cutoutMovingBlock());
		}

		ms.popPose();
	}

	public static PartialModel getBlazeModel(HeatLevel heatLevel, boolean blockAbove) {
		if (heatLevel.isAtLeast(HeatLevel.SEETHING)) {
			return blockAbove ? AllPartialModels.BLAZE_SUPER_ACTIVE : AllPartialModels.BLAZE_SUPER;
		} else if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			return blockAbove && heatLevel.isAtLeast(HeatLevel.KINDLED) ? AllPartialModels.BLAZE_ACTIVE
					: AllPartialModels.BLAZE_IDLE;
		} else {
			return AllPartialModels.BLAZE_INERT;
		}
	}

	private static void submit(SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, SubmitNodeCollector queue,
		RenderType renderType) {
		TransformStack.of(buffer.getTransforms())
				.rotateCentered(horizontalAngle, Direction.UP);
		buffer.light(LightCoordsUtil.FULL_BRIGHT)
				.submit(ms, renderType, queue);
	}
}
