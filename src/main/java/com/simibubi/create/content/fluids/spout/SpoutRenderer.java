package com.simibubi.create.content.fluids.spout;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class SpoutRenderer extends SafeBlockEntityRenderer<SpoutBlockEntity, SpoutRenderer.SpoutRenderState> {

	static final PartialModel[] BITS =
		{ AllPartialModels.SPOUT_TOP, AllPartialModels.SPOUT_MIDDLE, AllPartialModels.SPOUT_BOTTOM };

	public static class SpoutRenderState extends SafeRenderState {
		/** The fluid sitting in the spout's basin. */
		public @Nullable FluidBox reservoir;
		/** The droplet squeezed out while processing. */
		public @Nullable FluidBox droplet;
		public float squeeze;
		public final List<SuperByteBufferRenderState> bits = new ArrayList<>(BITS.length);
	}

	public record FluidBox(FluidStack fluid, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax,
		float verticalOffset, boolean renderBottom) {
	}

	public SpoutRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public SpoutRenderState createRenderState() {
		return new SpoutRenderState();
	}

	@Override
	protected void extractSafe(SpoutBlockEntity be, SpoutRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.reservoir = null;
		state.droplet = null;
		state.bits.clear();

		SmartFluidTankBehaviour tank = be.tank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
			boolean top = fluidStack.getFluid()
				.getFluidType()
				.isLighterThanAir();

			level = Math.max(level, 0.175f);
			float min = 2.5f / 16f;
			float max = min + (11 / 16f);
			float yOffset = (11 / 16f) * level;

			state.reservoir = new FluidBox(fluidStack.copy(), min, min - yOffset, min, max, min, max,
				top ? max - min : yOffset, false);
		}

		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = Mth.clamp(processingProgress, 0, 1);
		float radius = 0;

		if (!fluidStack.isEmpty() && processingTicks != -1) {
			radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			AABB bb = new AABB(0.5, 0.0, 0.5, 0.5, -1.2, 0.5).inflate(radius / 32f);
			state.droplet = new FluidBox(fluidStack.copy(), (float) bb.minX, (float) bb.minY, (float) bb.minZ,
				(float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, 0, true);
		}

		float squeeze = radius;
		if (processingPT < 0)
			squeeze = 0;
		else if (processingPT < 2)
			squeeze = Mth.lerp(processingPT / 2f, 0, -1);
		else if (processingPT < 10)
			squeeze = -1;
		state.squeeze = squeeze;

		for (PartialModel bit : BITS)
			state.bits.add(CachedBuffers.partial(bit, be.getBlockState())
				.light(state.lightCoords)
				.extractRenderState());
	}

	@Override
	protected void submitSafe(SpoutRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.reservoir != null)
			submitFluid(state.reservoir, ms, queue, state.lightCoords);
		if (state.droplet != null)
			submitFluid(state.droplet, ms, queue, state.lightCoords);

		ms.pushPose();
		for (SuperByteBufferRenderState bit : state.bits) {
			bit.submit(ms, RenderTypes.solidMovingBlock(), queue);
			ms.translate(0, -3 * state.squeeze / 32f, 0);
		}
		ms.popPose();
	}

	private static void submitFluid(FluidBox box, PoseStack ms, SubmitNodeCollector queue, int light) {
		ms.pushPose();
		ms.translate(0, box.verticalOffset(), 0);
		FluidRenderHelper.submitFluidBox(queue, box.fluid(), box.xMin(), box.yMin(), box.zMin(), box.xMax(),
			box.yMax(), box.zMax(), ms, light, box.renderBottom(), true);
		ms.popPose();
	}

}
