package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.client.level.VirtualBlockGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * Draws the bracket a shaft or cogwheel is wearing, and nothing else.
 * <p>
 * Wherever the block sits in a level, something else is already drawing the shaft: Flywheel in the
 * world, the block entity renderer in a ponder scene. Only when the model is asked for on its own,
 * detached from any level, does it have to supply the shaft itself.
 */
public class BracketedKineticBlockModel extends DelegateBlockStateModel {

	public BracketedKineticBlockModel(BlockStateModel template) {
		super(template);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		// A virtual getter stands in for "no level here": a GUI element, a ghost block, a model being
		// buffered on its own. There is nothing else to draw the shaft in that case, so draw it.
		if (level instanceof VirtualBlockGetter) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		BlockStateModel bracket = getBracket(level, pos);
		if (bracket != null)
			bracket.collectParts(level, pos, state, random, parts);
	}

	private static @Nullable BlockStateModel getBracket(BlockAndTintGetter world, BlockPos pos) {
		BracketedBlockEntityBehaviour attachmentBehaviour =
			BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
		if (attachmentBehaviour == null)
			return null;
		BlockState bracket = attachmentBehaviour.getBracket();
		if (bracket == null)
			return null;
		return Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(bracket);
	}

}
