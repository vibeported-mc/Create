package com.simibubi.create.content.logistics.tunnel;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class BeltTunnelRenderer
	extends SmartBlockEntityRenderer<BeltTunnelBlockEntity, BeltTunnelRenderer.BeltTunnelRenderState> {

	public static class BeltTunnelRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState flap;
		public final Map<Direction, Float> flapness = new EnumMap<>(Direction.class);
	}

	public BeltTunnelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BeltTunnelRenderState createRenderState() {
		return new BeltTunnelRenderState();
	}

	@Override
	protected void extractSafe(BeltTunnelBlockEntity be, BeltTunnelRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.flap = null;
		state.flapness.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		for (Direction direction : Iterate.directions) {
			if (!be.flaps.containsKey(direction))
				continue;
			state.flapness.put(direction, be.flaps.get(direction)
				.getValue(partialTicks));
		}

		if (state.flapness.isEmpty())
			return;

		// One extracted state serves every direction: the segments share geometry and differ only in
		// the PoseStack they are submitted with.
		SuperByteBuffer flapBuffer = CachedBufferer.partial(AllPartialModels.BELT_TUNNEL_FLAP, be.getBlockState());
		state.flap = flapBuffer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(BeltTunnelRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.flap == null)
			return;
		for (Map.Entry<Direction, Float> entry : state.flapness.entrySet())
			FlapStuffs.submitFlaps(ms, queue, state.flap, FlapStuffs.TUNNEL_PIVOT, entry.getKey(), entry.getValue(), 0);
	}

}
