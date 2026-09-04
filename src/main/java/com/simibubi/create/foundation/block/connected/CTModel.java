package com.simibubi.create.foundation.block.connected;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import com.simibubi.create.foundation.model.TransformedModelPart;
import com.tterrag.registrate.util.nullness.NonNullFunction;
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

	/**
	 * Wraps a block's model in a {@link CTModel} carrying this behaviour.
	 * <p>
	 * Here rather than at the call site in {@code CreateRegistrate}, and that placement is
	 * load-bearing. A lambda capturing the behaviour compiles to a synthetic method on whichever
	 * class writes it, and that method's descriptor names {@link BlockStateModel} - a class the
	 * dedicated server does not have. The JVM verifies a class as a whole when it is linked, so a
	 * common class carrying such a method cannot be loaded on a server at all, whether or not the
	 * method is ever called. Until 26.2 {@code @OnlyIn} stripped those members and the question did
	 * not arise; NeoForge no longer strips them, so the code has to sit where it belongs instead.
	 */
	public static NonNullFunction<BlockStateModel, ? extends BlockStateModel> swapper(
		ConnectedTextureBehaviour behaviour) {
		return model -> new CTModel(model, behaviour);
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
