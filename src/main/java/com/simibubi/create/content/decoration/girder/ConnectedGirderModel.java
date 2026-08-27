package com.simibubi.create.content.decoration.girder;

import java.util.List;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.block.connected.CTModel;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A girder, plus a bracket toward each neighbour it connects to.
 */
public class ConnectedGirderModel extends CTModel {

	public ConnectedGirderModel(BlockStateModel originalModel) {
		super(originalModel, new GirderCTBehaviour());
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		super.collectParts(level, pos, state, random, parts);
		for (Direction d : Iterate.horizontalDirections)
			if (GirderBlock.isConnected(level, pos, state, d))
				AllPartialModels.METAL_GIRDER_BRACKETS.get(d)
					.get()
					.collectParts(level, pos, state, random, parts);
	}

}
