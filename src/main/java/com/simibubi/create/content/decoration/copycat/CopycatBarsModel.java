package com.simibubi.create.content.decoration.copycat;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.jspecify.annotations.Nullable;
import com.simibubi.create.foundation.model.BakedModelHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;


public class CopycatBarsModel extends CopycatModel {

	public CopycatBarsModel(BlockStateModel originalModel) {
		super(originalModel);
	}

	@Override
	protected List<BakedQuad> getCroppedQuads(BlockState state, @Nullable Direction side, RandomSource rand,
		BlockState material, List<BlockStateModelPart> materialParts) {
		// The bars keep their own shape and only borrow the material's texture.
		List<BakedQuad> superQuads = BakedModelHelper.quadsOf(
			BakedModelHelper.collectParts(delegate, BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, rand), side);
		TextureAtlasSprite targetSprite = getModelOf(material).particleMaterial()
			.sprite();

		boolean vertical = state.getValue(CopycatPanelBlock.FACING)
			.getAxis() == Axis.Y;

		if (side != null && (vertical || side.getAxis() == Axis.Y)) {
			for (BakedQuad quad : BakedModelHelper.quadsOf(materialParts, null)) {
				if (quad.direction() != Direction.UP)
					continue;
				targetSprite = BakedQuadHelper.getSprite(quad);
				break;
			}
		}

		if (targetSprite == null)
			return superQuads;

		TextureAtlasSprite sprite = targetSprite;
		List<BakedQuad> quads = new ArrayList<>(superQuads.size());
		for (BakedQuad quad : superQuads)
			quads.add(BakedModelHelper.swapSprite(quad, ignored -> sprite));
		return quads;
	}

}
