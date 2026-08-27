package com.simibubi.create.content.kinetics.gauge;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock.Type;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
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

public class GaugeRenderer extends ShaftRenderer<GaugeBlockEntity, GaugeRenderer.GaugeRenderState> {

	public static class GaugeRenderState extends KineticBlockEntityRenderer.KineticRenderState {
		public final List<SuperByteBufferRenderState> faces = new ArrayList<>(8);
	}

	protected GaugeBlock.Type type;

	public static GaugeRenderer speed(BlockEntityRendererProvider.Context context) {
		return new GaugeRenderer(context, Type.SPEED);
	}

	public static GaugeRenderer stress(BlockEntityRendererProvider.Context context) {
		return new GaugeRenderer(context, Type.STRESS);
	}

	protected GaugeRenderer(BlockEntityRendererProvider.Context context, GaugeBlock.Type type) {
		super(context);
		this.type = type;
	}

	@Override
	public GaugeRenderState createRenderState() {
		return new GaugeRenderState();
	}

	@Override
	protected void extractSafe(GaugeBlockEntity be, GaugeRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.faces.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		super.extractSafe(be, state, partialTicks, cameraPosition);

		BlockState gaugeState = be.getBlockState();

		PartialModel partialModel =
			(type == Type.SPEED ? AllPartialModels.GAUGE_HEAD_SPEED : AllPartialModels.GAUGE_HEAD_STRESS);

		float dialPivot = 5.75f / 16;
		float progress = Mth.lerp(partialTicks, be.prevDialState, be.dialState);

		for (Direction facing : Iterate.directions) {
			if (!((GaugeBlock) gaugeState.getBlock()).shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), gaugeState,
				facing))
				continue;

			// Each face needs its own buffer now: a single buffer cannot carry two different
			// transforms across the extract/submit boundary.
			SuperByteBuffer dialBuffer = CachedBuffers.partial(AllPartialModels.GAUGE_DIAL, gaugeState);
			TransformStack.of(rotateBufferTowards(dialBuffer, facing).getTransforms())
				.translate(0, dialPivot, dialPivot)
				.rotate((float) (Math.PI / 2 * -progress), Direction.EAST)
				.translate(0, -dialPivot, -dialPivot);
			state.faces.add(dialBuffer.light(state.lightCoords)
				.extractRenderState());

			SuperByteBuffer headBuffer = CachedBuffers.partial(partialModel, gaugeState);
			state.faces.add(rotateBufferTowards(headBuffer, facing).light(state.lightCoords)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(GaugeRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState face : state.faces)
			face.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	protected SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
		TransformStack.of(buffer.getTransforms())
			.rotateCentered((float) ((-target.toYRot() - 90) / 180 * Math.PI), Direction.UP);
		return buffer;
	}

}
