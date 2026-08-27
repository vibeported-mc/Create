package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.function.UnaryOperator;

import org.joml.Vector3f;

import com.simibubi.create.foundation.model.BakedQuadHelper;
import com.simibubi.create.foundation.model.TransformedModelPart;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Tilts a track's model to the slope its block entity reports.
 */
public class TrackModel extends DelegateBlockStateModel {

	public TrackModel(BlockStateModel originalModel) {
		super(originalModel);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		int from = parts.size();
		super.collectParts(level, pos, state, random, parts);

		ModelData extraData = level.getModelData(pos);
		if (!extraData.has(TrackBlockEntityTilt.ASCENDING_PROPERTY))
			return;

		double angleIn = extraData.get(TrackBlockEntityTilt.ASCENDING_PROPERTY);
		double angle = Math.abs(angleIn);
		boolean flip = angleIn < 0;

		TrackShape trackShape = state.getValue(TrackBlock.SHAPE);
		double hAngle = switch (trackShape) {
			case XO -> 0;
			case PD -> 45;
			case ZO -> 90;
			case ND -> 135;
			default -> 0;
		};

		Vec3 verticalOffset = new Vec3(0, -0.25, 0);
		Vec3 diagonalRotationPoint =
			(trackShape == TrackShape.ND || trackShape == TrackShape.PD) ? new Vec3((Mth.SQRT_OF_TWO - 1) / 2, 0, 0)
				: Vec3.ZERO;

		UnaryOperator<Vec3> transform = v -> {
			v = v.add(verticalOffset);
			v = VecHelper.rotateCentered(v, hAngle, Axis.Y);
			v = v.add(diagonalRotationPoint);
			v = VecHelper.rotate(v, angle, Axis.Z);
			v = v.subtract(diagonalRotationPoint);
			v = VecHelper.rotateCentered(v, -hAngle + (flip ? 180 : 0), Axis.Y);
			v = v.subtract(verticalOffset);
			return v;
		};

		TransformedModelPart.wrapFrom(parts, from, (quad, cullFace, out) -> out.add(tilt(quad, transform)));
	}

	private static BakedQuad tilt(BakedQuad quad, UnaryOperator<Vec3> transform) {
		Vector3f[] positions = BakedQuadHelper.positions(quad);
		for (int vertex = 0; vertex < BakedQuadHelper.VERTEX_COUNT; vertex++)
			BakedQuadHelper.setXYZ(positions, vertex, transform.apply(BakedQuadHelper.getXYZ(positions, vertex)));
		return BakedQuadHelper.withGeometry(quad, positions, BakedQuadHelper.uvs(quad));
	}

}
