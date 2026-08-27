package com.simibubi.create.content.fluids.pipes;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class TransparentStraightPipeRenderer
	extends SafeBlockEntityRenderer<StraightPipeBlockEntity, TransparentStraightPipeRenderer.PipeRenderState> {

	public static class PipeRenderState extends SafeRenderState {
		public final List<StreamRenderState> streams = new ArrayList<>(6);
	}

	public record StreamRenderState(FluidStack fluid, Direction side, float progress, boolean inbound) {
	}

	public TransparentStraightPipeRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public PipeRenderState createRenderState() {
		return new PipeRenderState();
	}

	@Override
	protected void extractSafe(StraightPipeBlockEntity be, PipeRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.streams.clear();

		FluidTransportBehaviour pipe = be.getBehaviour(FluidTransportBehaviour.TYPE);
		if (pipe == null)
			return;

		for (Direction side : Iterate.directions) {

			Flow flow = pipe.getFlow(side);
			if (flow == null)
				continue;
			FluidStack fluidStack = flow.fluid;
			if (fluidStack.isEmpty())
				continue;
			LerpedFloat progress = flow.progress;
			if (progress == null)
				continue;

			float value = progress.getValue(partialTicks);
			boolean inbound = flow.inbound;
			if (value == 1) {
				if (inbound) {
					Flow opposite = pipe.getFlow(side.getOpposite());
					if (opposite == null)
						value -= 1e-6f;
				} else {
					FluidTransportBehaviour adjacent = BlockEntityBehaviour.get(be.getLevel(), be.getBlockPos()
						.relative(side), FluidTransportBehaviour.TYPE);
					if (adjacent == null)
						value -= 1e-6f;
					else {
						Flow other = adjacent.getFlow(side.getOpposite());
						if (other == null || !other.inbound && !other.complete)
							value -= 1e-6f;
					}
				}
			}

			state.streams.add(new StreamRenderState(fluidStack.copy(), side, value, inbound));
		}
	}

	@Override
	protected void submitSafe(PipeRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (StreamRenderState stream : state.streams)
			FluidRenderer.submitFluidStream(stream.fluid(), stream.side(), 3 / 16f, stream.progress(),
				stream.inbound(), queue, ms, state.lightCoords);
	}

}
