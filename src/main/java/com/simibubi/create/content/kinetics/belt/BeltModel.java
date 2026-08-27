package com.simibubi.create.content.kinetics.belt;

import java.util.List;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import com.simibubi.create.foundation.model.TransformedModelPart;

import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * Draws a belt with its casing and cover.
 * <p>
 * The casing type and cover still travel as model data from the belt's block entity; what changed is
 * the drawing. 26.2 collects a model into parts, so the cover is another part, and the andesite
 * casing's sprite swap rewrites quads as they are collected rather than editing vertex data.
 */
public class BeltModel extends DelegateBlockStateModel {

	public static final ModelProperty<CasingType> CASING_PROPERTY = new ModelProperty<>();
	public static final ModelProperty<Boolean> COVER_PROPERTY = new ModelProperty<>();

	private static final SpriteShiftEntry SPRITE_SHIFT = AllSpriteShifts.ANDESIDE_BELT_CASING;

	public BeltModel(BlockStateModel template) {
		super(template);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		ModelData data = level.getModelData(pos);
		if (!data.has(CASING_PROPERTY))
			return super.particleMaterial(level, pos, state);
		CasingType type = data.get(CASING_PROPERTY);
		if (type == CasingType.NONE || type == CasingType.BRASS)
			return super.particleMaterial(level, pos, state);
		return new Material.Baked(AllSpriteShifts.ANDESITE_CASING.getOriginal(), false);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		int from = parts.size();
		super.collectParts(level, pos, state, random, parts);

		ModelData data = level.getModelData(pos);
		if (!data.has(CASING_PROPERTY))
			return;

		boolean cover = Boolean.TRUE.equals(data.get(COVER_PROPERTY));
		CasingType type = data.get(CASING_PROPERTY);
		boolean brassCasing = type == CasingType.BRASS;

		if (type == CasingType.NONE || brassCasing && !cover)
			return;

		if (cover) {
			boolean alongX = state.getValue(BeltBlock.HORIZONTAL_FACING)
				.getAxis() == Axis.X;
			BlockStateModel coverModel =
				(brassCasing ? alongX ? AllPartialModels.BRASS_BELT_COVER_X : AllPartialModels.BRASS_BELT_COVER_Z
					: alongX ? AllPartialModels.ANDESITE_BELT_COVER_X : AllPartialModels.ANDESITE_BELT_COVER_Z).get();
			coverModel.collectParts(level, pos, state, random, parts);
		}

		if (brassCasing)
			return;

		TransformedModelPart.wrapFrom(parts, from, (quad, cullFace, out) -> out.add(shiftCasing(quad)));
	}

	/**
	 * Moves the belt casing onto the andesite variant of its sprite.
	 */
	private static BakedQuad shiftCasing(BakedQuad quad) {
		TextureAtlasSprite original = BakedQuadHelper.getSprite(quad);
		if (original != SPRITE_SHIFT.getOriginal())
			return quad;

		long[] uvs = BakedQuadHelper.uvs(quad);
		for (int vertex = 0; vertex < BakedQuadHelper.VERTEX_COUNT; vertex++) {
			float u = BakedQuadHelper.getU(uvs, vertex);
			float v = BakedQuadHelper.getV(uvs, vertex);
			BakedQuadHelper.setU(uvs, vertex, SPRITE_SHIFT.getTargetU(u));
			BakedQuadHelper.setV(uvs, vertex, SPRITE_SHIFT.getTargetV(v));
		}
		return BakedQuadHelper.withGeometry(quad, BakedQuadHelper.positions(quad), uvs);
	}

}
