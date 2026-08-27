package com.simibubi.create.content.fluids.tank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fluid tank, with the faces it shares with a neighbouring tank left out.
 * <p>
 * The tank hides those faces itself rather than letting the chunk mesher cull them, so everything is
 * handed out unculled: 26.2 asks a part for its quads per cull face, and the culled buckets here are
 * folded into the unculled one.
 */
public class FluidTankModel extends CTModel {

	public static FluidTankModel standard(BlockStateModel originalModel) {
		return new FluidTankModel(originalModel, AllSpriteShifts.FLUID_TANK, AllSpriteShifts.FLUID_TANK_TOP,
			AllSpriteShifts.FLUID_TANK_INNER);
	}

	public static FluidTankModel creative(BlockStateModel originalModel) {
		return new FluidTankModel(originalModel, AllSpriteShifts.CREATIVE_FLUID_TANK, AllSpriteShifts.CREATIVE_CASING,
			AllSpriteShifts.CREATIVE_CASING);
	}

	private FluidTankModel(BlockStateModel originalModel, CTSpriteShiftEntry side, CTSpriteShiftEntry top,
		CTSpriteShiftEntry inner) {
		super(originalModel, new FluidTankCTBehaviour(side, top, inner));
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		CullData cullData = new CullData();
		for (Direction d : Iterate.horizontalDirections)
			cullData.setCulled(d, ConnectivityHandler.isConnected(level, pos, pos.relative(d)));

		List<BlockStateModelPart> collected = new ArrayList<>();
		super.collectParts(level, pos, state, random, collected);
		for (BlockStateModelPart part : collected)
			parts.add(new UnculledPart(part, cullData));
	}

	/**
	 * Hands every quad out unculled, minus the faces shared with a neighbouring tank.
	 */
	private record UnculledPart(BlockStateModelPart delegate, CullData cullData) implements BlockStateModelPart {

		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			if (direction != null)
				return List.of();

			List<BakedQuad> quads = new ArrayList<>();
			for (Direction d : Iterate.directions) {
				if (cullData.isCulled(d))
					continue;
				quads.addAll(delegate.getQuads(d));
			}
			quads.addAll(delegate.getQuads(null));
			return quads;
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public TriState ambientOcclusion() {
			return delegate.ambientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}

	}

	private static class CullData {
		boolean[] culledFaces;

		public CullData() {
			culledFaces = new boolean[4];
			Arrays.fill(culledFaces, false);
		}

		void setCulled(Direction face, boolean cull) {
			if (face.getAxis()
				.isVertical())
				return;
			culledFaces[face.get2DDataValue()] = cull;
		}

		boolean isCulled(Direction face) {
			if (face.getAxis()
				.isVertical())
				return false;
			return culledFaces[face.get2DDataValue()];
		}
	}

}
