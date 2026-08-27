package com.simibubi.create.content.trains.track;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_BOTTOM;
import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_MIDDLE;
import static com.simibubi.create.AllPartialModels.GIRDER_SEGMENT_TOP;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.content.trains.track.BezierConnection.GirderAngles;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrackRenderer extends SafeBlockEntityRenderer<TrackBlockEntity, TrackRenderer.TrackRenderState> {

	public static class TrackRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> segments = new ArrayList<>();
	}

	public TrackRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public TrackRenderState createRenderState() {
		return new TrackRenderState();
	}

	@Override
	protected void extractSafe(TrackBlockEntity be, TrackRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.segments.clear();

		Level level = be.getLevel();
		if (VisualizationManager.supportsVisualization(level)) {
			state.skip = true;
			return;
		}

		be.connections.values()
			.forEach(bc -> extractBezierTurn(level, bc, state.segments));
	}

	@Override
	protected void submitSafe(TrackRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		ms.pushPose();
		for (SuperByteBufferRenderState segment : state.segments)
			segment.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		ms.popPose();
	}

	/**
	 * Each segment of a curve carries its own baked transform, so each needs its own extracted buffer:
	 * one state cannot hold several poses.
	 */
	public static void extractBezierTurn(Level level, BezierConnection bc, List<SuperByteBufferRenderState> out) {
		if (!bc.isPrimary())
			return;

		BlockPos bePosition = bc.bePositions.getFirst();
		BlockState air = Blocks.AIR.defaultBlockState();
		SegmentAngles segment = bc.getBakedSegments();

		extractGirder(level, bc, out, bePosition);

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(bePosition));

			TrackMaterial.TrackModelHolder modelHolder = bc.getMaterial().getModelHolder();

			out.add(transformed(modelHolder.tie(), air, segment.tieTransform[i], light));

			for (boolean first : Iterate.trueAndFalse) {
				Pose transform = segment.railTransforms[i].get(first);
				out.add(transformed(first ? modelHolder.leftSegment() : modelHolder.rightSegment(), air, transform,
					light));
			}
		}
	}

	private static void extractGirder(Level level, BezierConnection bc, List<SuperByteBufferRenderState> out,
		BlockPos tePosition) {
		if (!bc.hasGirder)
			return;

		BlockState air = Blocks.AIR.defaultBlockState();
		GirderAngles segment = bc.getBakedGirders();

		for (int i = 1; i < segment.length; i++) {
			int light = LightCoordsUtil.getLightCoords(level, segment.lightPosition[i].offset(tePosition));

			for (boolean first : Iterate.trueAndFalse) {
				out.add(transformed(GIRDER_SEGMENT_MIDDLE, air, segment.beams[i].get(first), light));

				for (boolean top : Iterate.trueAndFalse) {
					Pose beamCapTransform = segment.beamCaps[i].get(top)
						.get(first);
					out.add(transformed(top ? GIRDER_SEGMENT_TOP : GIRDER_SEGMENT_BOTTOM, air, beamCapTransform,
						light));
				}
			}
		}
	}

	/**
	 * A Pose carries both the model and normal matrices, which is what the old mulPose/mulNormal pair
	 * applied one at a time.
	 */
	private static SuperByteBufferRenderState transformed(PartialModel model, BlockState air, Pose transform,
		int light) {
		SuperByteBuffer buffer = CachedBuffers.partial(model, air);
		TransformStack.of(buffer.getTransforms())
			.transform(transform);
		return buffer.light(light)
			.extractRenderState();
	}

	public static Vec3 getModelAngles(Vec3 normal, Vec3 diff) {
		double diffX = diff.x();
		double diffY = diff.y();
		double diffZ = diff.z();
		double len = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
		double yaw = Mth.atan2(diffX, diffZ);
		double pitch = Mth.atan2(len, diffY) - Math.PI * .5;

		Vec3 yawPitchNormal = VecHelper.rotate(VecHelper.rotate(new Vec3(0, 1, 0), AngleHelper.deg(pitch), Axis.X),
			AngleHelper.deg(yaw), Axis.Y);

		double signum = Math.signum(yawPitchNormal.dot(normal));
		if (Math.abs(signum) < 0.5f)
			signum = yawPitchNormal.distanceToSqr(normal) < 0.5f ? -1 : 1;
		double dot = diff.cross(normal)
			.normalize()
			.dot(yawPitchNormal);
		double roll = Math.acos(Mth.clamp(dot, -1, 1)) * signum;
		return new Vec3(pitch, yaw, roll);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96 * 2;
	}

}
