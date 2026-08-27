package com.simibubi.create.foundation.model;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.joml.Vector3f;
import static net.createmod.catnip.api.client.render.SpriteShiftEntry.getUnInterpolatedU;
import static net.createmod.catnip.api.client.render.SpriteShiftEntry.getUnInterpolatedV;

import java.util.List;
import java.util.function.UnaryOperator;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BakedModelHelper {

	/**
	 * Clips a quad to {@code crop} and shifts it by {@code move}, dragging its texture along.
	 */
	public static BakedQuad cropAndMove(BakedQuad quad, AABB crop, Vec3 move) {
		TextureAtlasSprite sprite = BakedQuadHelper.getSprite(quad);
		Vector3f[] positions = BakedQuadHelper.positions(quad);
		long[] uvs = BakedQuadHelper.uvs(quad);

		Vec3 xyz0 = BakedQuadHelper.getXYZ(positions, 0);
		Vec3 xyz1 = BakedQuadHelper.getXYZ(positions, 1);
		Vec3 xyz2 = BakedQuadHelper.getXYZ(positions, 2);
		Vec3 xyz3 = BakedQuadHelper.getXYZ(positions, 3);

		Vec3 uAxis = xyz3.add(xyz2)
			.scale(.5);
		Vec3 vAxis = xyz1.add(xyz2)
			.scale(.5);
		Vec3 center = xyz3.add(xyz2)
			.add(xyz0)
			.add(xyz1)
			.scale(.25);

		float u0 = BakedQuadHelper.getU(uvs, 0);
		float u3 = BakedQuadHelper.getU(uvs, 3);
		float v0 = BakedQuadHelper.getV(uvs, 0);
		float v1 = BakedQuadHelper.getV(uvs, 1);

		float uScale = (float) Math
			.round((getUnInterpolatedU(sprite, u3) - getUnInterpolatedU(sprite, u0)) / xyz3.distanceTo(xyz0));
		float vScale = (float) Math
			.round((getUnInterpolatedV(sprite, v1) - getUnInterpolatedV(sprite, v0)) / xyz1.distanceTo(xyz0));

		if (uScale == 0) {
			float v3 = BakedQuadHelper.getV(uvs, 3);
			float u1 = BakedQuadHelper.getU(uvs, 1);
			uAxis = xyz1.add(xyz2)
				.scale(.5);
			vAxis = xyz3.add(xyz2)
				.scale(.5);
			uScale = (float) Math
				.round((getUnInterpolatedU(sprite, u1) - getUnInterpolatedU(sprite, u0)) / xyz1.distanceTo(xyz0));
			vScale = (float) Math
				.round((getUnInterpolatedV(sprite, v3) - getUnInterpolatedV(sprite, v0)) / xyz3.distanceTo(xyz0));

		}

		uAxis = uAxis.subtract(center)
			.normalize();
		vAxis = vAxis.subtract(center)
			.normalize();

		Vec3 min = new Vec3(crop.minX, crop.minY, crop.minZ);
		Vec3 max = new Vec3(crop.maxX, crop.maxY, crop.maxZ);

		for (int vertex = 0; vertex < BakedQuadHelper.VERTEX_COUNT; vertex++) {
			Vec3 xyz = BakedQuadHelper.getXYZ(positions, vertex);
			Vec3 newXyz = VecHelper.componentMin(max, VecHelper.componentMax(xyz, min));
			Vec3 diff = newXyz.subtract(xyz);

			if (diff.lengthSqr() > 0) {
				float u = BakedQuadHelper.getU(uvs, vertex);
				float v = BakedQuadHelper.getV(uvs, vertex);
				float uDiff = (float) uAxis.dot(diff) * uScale;
				float vDiff = (float) vAxis.dot(diff) * vScale;
				BakedQuadHelper.setU(uvs, vertex, sprite.getU(getUnInterpolatedU(sprite, u) + uDiff));
				BakedQuadHelper.setV(uvs, vertex, sprite.getV(getUnInterpolatedV(sprite, v) + vDiff));
			}

			BakedQuadHelper.setXYZ(positions, vertex, newXyz.add(move));
		}

		return BakedQuadHelper.withGeometry(quad, positions, uvs);
	}

	/**
	 * Every part a model contributes at a position.
	 */
	public static List<BlockStateModelPart> collectParts(BlockStateModel model, BlockAndTintGetter level, BlockPos pos,
		BlockState state, RandomSource random) {
		List<BlockStateModelPart> parts = new java.util.ArrayList<>();
		model.collectParts(level, pos, state, random, parts);
		return parts;
	}

	/**
	 * Every quad those parts contribute for one cull face.
	 */
	public static List<BakedQuad> quadsOf(List<BlockStateModelPart> parts, @org.jspecify.annotations.Nullable Direction side) {
		if (parts.size() == 1)
			return parts.getFirst()
				.getQuads(side);
		List<BakedQuad> quads = new java.util.ArrayList<>();
		for (BlockStateModelPart part : parts)
			quads.addAll(part.getQuads(side));
		return quads;
	}

	/**
	 * A copy of {@code template} drawn from different sprites.
	 */
	public static BlockStateModel generateModel(BlockStateModel template,
		UnaryOperator<TextureAtlasSprite> spriteSwapper) {
		return new DelegateBlockStateModel(template) {
			@Override
			public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
				List<BlockStateModelPart> parts) {
				int from = parts.size();
				super.collectParts(level, pos, state, random, parts);
				TransformedModelPart.wrapFrom(parts, from,
					(quad, cullFace, out) -> out.add(swapSprite(quad, spriteSwapper)));
			}

			@Override
			@SuppressWarnings("deprecation")
			public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
				int from = parts.size();
				super.collectParts(random, parts);
				TransformedModelPart.wrapFrom(parts, from,
					(quad, cullFace, out) -> out.add(swapSprite(quad, spriteSwapper)));
			}
		};
	}

	public static BakedQuad swapSprite(BakedQuad quad, UnaryOperator<TextureAtlasSprite> spriteSwapper) {
		TextureAtlasSprite sprite = BakedQuadHelper.getSprite(quad);
		TextureAtlasSprite newSprite = spriteSwapper.apply(sprite);
		if (newSprite == null || sprite == newSprite)
			return quad;

		long[] uvs = BakedQuadHelper.uvs(quad);
		for (int vertex = 0; vertex < BakedQuadHelper.VERTEX_COUNT; vertex++) {
			float u = BakedQuadHelper.getU(uvs, vertex);
			float v = BakedQuadHelper.getV(uvs, vertex);
			BakedQuadHelper.setU(uvs, vertex, newSprite.getU(getUnInterpolatedU(sprite, u)));
			BakedQuadHelper.setV(uvs, vertex, newSprite.getV(getUnInterpolatedV(sprite, v)));
		}
		return BakedQuadHelper.withSprite(BakedQuadHelper.withGeometry(quad, BakedQuadHelper.positions(quad), uvs),
			newSprite);
	}

}
