package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class FluidTankRenderer
	extends SafeBlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.FluidTankRenderState> {

	public static class FluidTankRenderState extends SafeRenderState {
		public @Nullable FluidBox fluid;
		public final List<SuperByteBufferRenderState> gauges = new ArrayList<>();
		/** The multiblock's footprint; the gauges are placed relative to its centre. */
		public int width = 1;
	}

	/**
	 * The fluid box is drawn by Catnip at submit time, so only its extents and contents are carried.
	 */
	public record FluidBox(FluidStack fluid, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax,
		float verticalOffset) {
	}

	public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public FluidTankRenderState createRenderState() {
		return new FluidTankRenderState();
	}

	@Override
	protected void extractSafe(FluidTankBlockEntity be, FluidTankRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.fluid = null;
		state.gauges.clear();

		if (!be.isController())
			return;
		if (!be.window) {
			if (be.boiler.isActive())
				extractBoilerGauges(be, state, partialTicks);
			return;
		}

		LerpedFloat fluidLevel = be.getFluidLevel();
		if (fluidLevel == null)
			return;

		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = be.height - 2 * capHeight - minPuddleHeight;

		float level = fluidLevel.getValue(partialTicks);
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

		FluidStack fluidStack = be.tankInventory.getFluid();
		if (fluidStack.isEmpty())
			return;

		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		float xMin = tankHullWidth;
		float xMax = xMin + be.width - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + be.width - 2 * tankHullWidth;

		state.fluid = new FluidBox(fluidStack.copy(), xMin, yMin, zMin, xMax, yMax, zMax,
			clampedLevel - totalHeight);
	}

	protected void extractBoilerGauges(FluidTankBlockEntity be, FluidTankRenderState state, float partialTicks) {
		BlockState blockState = be.getBlockState();
		state.width = be.width;

		float dialPivotY = 6f / 16;
		float dialPivotZ = 8f / 16;
		float progress = be.boiler.gauge.getValue(partialTicks);

		for (Direction d : Iterate.horizontalDirections) {
			if (be.boiler.occludedDirections[d.get2DDataValue()])
				continue;
			float yRot = -d.toYRot() - 90;

			SuperByteBuffer gauge = CachedBufferer.partial(AllPartialModels.BOILER_GAUGE, blockState);
			TransformStack.of(gauge.getTransforms())
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(be.width / 2f - 6 / 16f, 0, 0);
			state.gauges.add(gauge.light(state.lightCoords)
				.extractRenderState());

			SuperByteBuffer dial = CachedBufferer.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState);
			TransformStack.of(dial.getTransforms())
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(be.width / 2f - 6 / 16f, 0, 0)
				.translate(0, dialPivotY, dialPivotZ)
				.rotateXDegrees(-145 * progress + 90)
				.translate(0, -dialPivotY, -dialPivotZ);
			state.gauges.add(dial.light(state.lightCoords)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(FluidTankRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.fluid != null) {
			FluidBox box = state.fluid;
			ms.pushPose();
			ms.translate(0, box.verticalOffset(), 0);
			FluidRenderHelper.submitFluidBox(queue, box.fluid(), box.xMin(), box.yMin(), box.zMin(), box.xMax(),
				box.yMax(), box.zMax(), ms, state.lightCoords, false, true);
			ms.popPose();
		}

		if (state.gauges.isEmpty())
			return;

		ms.pushPose();
		TransformStack.of(ms)
			.translate(state.width / 2f, 0.5, state.width / 2f);
		for (SuperByteBufferRenderState gauge : state.gauges)
			gauge.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		ms.popPose();
	}

	/**
	 * 26.2 dropped the block entity argument, so this can no longer be narrowed to controllers only.
	 * Non-controllers extract nothing, so the difference is a wasted visibility check, not extra
	 * geometry.
	 */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

}
