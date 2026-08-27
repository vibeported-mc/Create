package com.simibubi.create.content.decoration.copycat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import com.simibubi.create.foundation.model.TransformedModelPart;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * Draws a copycat block using the model of whatever material it is wearing.
 * <p>
 * Minecraft 26.2 collects a model into parts instead of asking for quads face by face, and it hands
 * the level and position straight to {@link #collectParts}, so the material and the occlusion it
 * causes are worked out there rather than travelling as model data.
 */
public abstract class CopycatModel extends DelegateBlockStateModel {

	public static final ModelProperty<BlockState> MATERIAL_PROPERTY = new ModelProperty<>();

	public CopycatModel(BlockStateModel originalModel) {
		super(originalModel);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		BlockState material = getMaterial(level.getModelData(pos));

		if (!(state.getBlock() instanceof CopycatBlock copycatBlock)) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		OcclusionData occlusionData = new OcclusionData();
		gatherOcclusionData(level, pos, state, material, occlusionData, copycatBlock);

		// The block's own model shows through only where the material is hidden.
		int baseFrom = parts.size();
		super.collectParts(level, pos, state, random, parts);
		TransformedModelPart.wrapFrom(parts, baseFrom, (quad, cullFace, out) -> {
			if (occlusionData.isOccluded(cullFace))
				out.add(quad);
		});

		// The material only sees the neighbours the copycat is willing to connect to.
		BlockAndTintGetter filtered = new FilteredBlockAndTintGetter(level,
			targetPos -> copycatBlock.canConnectTexturesToward(level, pos, targetPos, state));
		List<BlockStateModelPart> materialParts =
			BakedModelHelper.collectParts(getModelOf(material), filtered, pos, material, random);

		parts.add(new CroppedPart(state, random, material, materialParts, occlusionData, copycatBlock,
			material.emissiveRendering()));
	}

	private void gatherOcclusionData(BlockAndTintGetter level, BlockPos pos, BlockState state, BlockState material,
		OcclusionData occlusionData, CopycatBlock copycatBlock) {
		MutableBlockPos mutablePos = new MutableBlockPos();
		for (Direction face : Iterate.directions) {

			MutableBlockPos neighbourPos = mutablePos.setWithOffset(pos, face);
			BlockState neighbourState = level.getBlockState(neighbourPos);
			if (state.supportsExternalFaceHiding()
				&& neighbourState.hidesNeighborFace(level, neighbourPos, state, face.getOpposite())) {
				occlusionData.occlude(face);
				continue;
			}

			if (!copycatBlock.canFaceBeOccluded(state, face))
				continue;
			if (!Block.shouldRenderFace(level, pos, material, neighbourState, face))
				occlusionData.occlude(face);
		}
	}

	/**
	 * The material's quads, cropped to the shape this copycat wears them in.
	 * <p>
	 * The returned list must not be mutated.
	 */
	protected abstract List<BakedQuad> getCroppedQuads(BlockState state, @Nullable Direction side, RandomSource rand,
		BlockState material, List<BlockStateModelPart> materialParts);

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return getModelOf(getMaterial(level.getModelData(pos))).particleMaterial();
	}

	@NotNull
	public static BlockState getMaterial(ModelData data) {
		BlockState material = data == null ? null : data.get(MATERIAL_PROPERTY);
		return material == null ? AllBlocks.COPYCAT_BASE.getDefaultState() : material;
	}

	public static BlockStateModel getModelOf(BlockState state) {
		return Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(state);
	}

	/**
	 * The material's geometry, as a single part.
	 * <p>
	 * Faces the copycat always renders are moved into the unculled bucket rather than being handed
	 * out under a cull face that would drop them against a solid neighbour.
	 */
	private final class CroppedPart implements BlockStateModelPart {

		private final BlockState state;
		private final RandomSource random;
		private final BlockState material;
		private final List<BlockStateModelPart> materialParts;
		private final OcclusionData occlusionData;
		private final CopycatBlock copycatBlock;
		private final boolean emissive;

		private final Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
		private @Nullable List<BakedQuad> unculled;

		private CroppedPart(BlockState state, RandomSource random, BlockState material,
			List<BlockStateModelPart> materialParts, OcclusionData occlusionData, CopycatBlock copycatBlock,
			boolean emissive) {
			this.state = state;
			this.random = random;
			this.material = material;
			this.materialParts = materialParts;
			this.occlusionData = occlusionData;
			this.copycatBlock = copycatBlock;
			this.emissive = emissive;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			if (direction == null) {
				if (unculled == null) {
					List<BakedQuad> quads = new ArrayList<>(cropped(null));
					for (Direction face : Iterate.directions)
						if (copycatBlock.shouldFaceAlwaysRender(state, face))
							quads.addAll(cropped(face));
					unculled = quads;
				}
				return unculled;
			}

			if (occlusionData.isOccluded(direction) || copycatBlock.shouldFaceAlwaysRender(state, direction))
				return List.of();
			return culled.computeIfAbsent(direction, this::cropped);
		}

		private List<BakedQuad> cropped(@Nullable Direction side) {
			List<BakedQuad> quads = getCroppedQuads(state, side, random, material, materialParts);
			if (!emissive || quads.isEmpty())
				return quads;
			// Vanilla has no per-quad levels of emissivity, so the whole material lights up at once.
			List<BakedQuad> lit = new ArrayList<>(quads.size());
			for (BakedQuad quad : quads)
				lit.add(BakedQuadHelper.withMaxEmissivity(quad));
			return lit;
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean useAmbientOcclusion() {
			return materialParts.isEmpty() || materialParts.getFirst()
				.useAmbientOcclusion();
		}

		@Override
		public TriState ambientOcclusion() {
			return materialParts.isEmpty() ? TriState.DEFAULT
				: materialParts.getFirst()
					.ambientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return getModelOf(material).particleMaterial();
		}

		@Override
		public int materialFlags() {
			int flags = 0;
			for (BlockStateModelPart part : materialParts)
				flags |= part.materialFlags();
			return flags;
		}

	}

	private static class OcclusionData {
		private final boolean[] occluded;

		public OcclusionData() {
			occluded = new boolean[6];
		}

		public void occlude(Direction face) {
			occluded[face.get3DDataValue()] = true;
		}

		public boolean isOccluded(@Nullable Direction face) {
			return face != null && occluded[face.get3DDataValue()];
		}
	}

}
