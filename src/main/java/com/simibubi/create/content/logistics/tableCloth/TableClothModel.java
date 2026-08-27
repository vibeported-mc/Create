package com.simibubi.create.content.logistics.tableCloth;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.TransformedModelPart;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * A table cloth, with a draped corner added wherever it hangs over an edge.
 */
public class TableClothModel extends DelegateBlockStateModel {

	private static final Map<TableClothBlock, List<List<BakedQuad>>> CORNERS = new HashMap<>();

	public TableClothModel(BlockStateModel originalModel) {
		super(originalModel);
	}

	public static void reload() {
		CORNERS.clear();
	}

	private List<BakedQuad> getCorner(TableClothBlock block, int corner, RandomSource rand) {
		if (!CORNERS.containsKey(block)) {
			TextureAtlasSprite targetSprite = particleMaterial().sprite();
			List<List<BakedQuad>> list = new ArrayList<>();

			for (PartialModel pm : List.of(AllPartialModels.TABLE_CLOTH_SW, AllPartialModels.TABLE_CLOTH_NW,
				AllPartialModels.TABLE_CLOTH_NE, AllPartialModels.TABLE_CLOTH_SE))
				list.add(getCornerQuads(rand, targetSprite, pm));

			CORNERS.put(block, list);
		}

		return CORNERS.get(block)
			.get(corner);
	}

	private List<BakedQuad> getCornerQuads(RandomSource rand, TextureAtlasSprite targetSprite, PartialModel pm) {
		List<BlockStateModelPart> parts = new ArrayList<>();
		pm.get()
			.collectParts(rand, parts);

		List<BakedQuad> quads = new ArrayList<>();
		for (BakedQuad quad : BakedModelHelper.quadsOf(parts, null))
			quads.add(BakedModelHelper.swapSprite(quad, ignored -> targetSprite));
		return quads;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		int from = parts.size();
		super.collectParts(level, pos, state, random, parts);
		TransformedModelPart.forceAmbientOcclusion(parts, from, TriState.FALSE);

		if (!(state.getBlock() instanceof TableClothBlock dcb))
			return;

		EnumSet<Direction> culled = EnumSet.noneOf(Direction.class);
		for (Direction side : Iterate.horizontalDirections)
			if (!Block.shouldRenderFace(level, pos, state, level.getBlockState(pos.relative(side)), side))
				culled.add(side);

		parts.add(new CornerPart(dcb, culled, random));
	}

	/**
	 * The draped corners, one per horizontal side the cloth hangs over.
	 */
	private final class CornerPart implements BlockStateModelPart {

		private final TableClothBlock block;
		private final EnumSet<Direction> culled;
		private final RandomSource random;

		private CornerPart(TableClothBlock block, EnumSet<Direction> culled, RandomSource random) {
			this.block = block;
			this.culled = culled;
			this.random = random;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			if (direction == null || direction.getAxis() == Axis.Y)
				return List.of();
			if (culled.contains(direction.getClockWise()))
				return List.of();
			return getCorner(block, direction.get2DDataValue(), random);
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean useAmbientOcclusion() {
			return false;
		}

		@Override
		public TriState ambientOcclusion() {
			return TriState.FALSE;
		}

		@Override
		public Material.Baked particleMaterial() {
			return TableClothModel.this.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return TableClothModel.this.materialFlags();
		}

	}

}
