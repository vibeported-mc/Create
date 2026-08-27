package com.simibubi.create.foundation.block.connected;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import com.simibubi.create.foundation.model.TransformedModelPart;
import java.util.Arrays;
import java.util.List;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CTModel extends DelegateBlockStateModel {

	private final ConnectedTextureBehaviour behaviour;

	public CTModel(BlockStateModel originalModel, ConnectedTextureBehaviour behaviour) {
		super(originalModel);
		this.behaviour = behaviour;
	}

	protected CTData createCTData(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		CTData data = new CTData();
		MutableBlockPos mutablePos = new MutableBlockPos();
		for (Direction face : Iterate.directions) {
			BlockState actualState = world.getBlockState(pos);
			if (!behaviour.buildContextForOccludedDirections()
				&& !Block.shouldRenderFace(world, pos, state,
					world.getBlockState(mutablePos.setWithOffset(pos, face)), face)
				&& !(actualState.getBlock()instanceof CopycatBlock ufb
					&& !ufb.canFaceBeOccluded(actualState, face)))
				continue;
			CTType dataType = behaviour.getDataType(world, pos, state, face);
			if (dataType == null)
				continue;
			CTContext context = behaviour.buildContext(world, pos, state, face, dataType.getContextRequirement());
			data.put(face, dataType.getTextureIndex(context));
		}
		return data;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		int from = parts.size();
		super.collectParts(level, pos, state, random, parts);
		CTData data = createCTData(level, pos, state);
		TransformedModelPart.wrapFrom(parts, from, (quad, cullFace, out) -> out.add(shift(quad, data, state, random)));
	}

	/**
	 * Moves a quad onto the connected-texture variant its face resolved to.
	 */
	private BakedQuad shift(BakedQuad quad, CTData data, BlockState state, RandomSource random) {
		int index = data.get(quad.direction());
		if (index == -1)
			return quad;

		TextureAtlasSprite sprite = BakedQuadHelper.getSprite(quad);
		CTSpriteShiftEntry spriteShift = behaviour.getShift(state, random, quad.direction(), sprite);
		if (spriteShift == null || sprite != spriteShift.getOriginal())
			return quad;

		long[] uvs = BakedQuadHelper.uvs(quad);
		for (int vertex = 0; vertex < BakedQuadHelper.VERTEX_COUNT; vertex++) {
			float u = BakedQuadHelper.getU(uvs, vertex);
			float v = BakedQuadHelper.getV(uvs, vertex);
			BakedQuadHelper.setU(uvs, vertex, spriteShift.getTargetU(u, index));
			BakedQuadHelper.setV(uvs, vertex, spriteShift.getTargetV(v, index));
		}
		return BakedQuadHelper.withGeometry(quad, BakedQuadHelper.positions(quad), uvs);
	}

	private static class CTData {
		private final int[] indices;

		public CTData() {
			indices = new int[6];
			Arrays.fill(indices, -1);
		}

		public void put(Direction face, int texture) {
			indices[face.get3DDataValue()] = texture;
		}

		public int get(Direction face) {
			return indices[face.get3DDataValue()];
		}
	}

}
