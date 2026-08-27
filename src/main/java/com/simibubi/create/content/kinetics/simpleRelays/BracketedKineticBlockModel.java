package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.impl.neoforge.render.VirtualRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Draws the bracket a shaft or cogwheel is wearing, and nothing else.
 * <p>
 * The shaft itself is drawn by Flywheel, so in the world this model contributes only the bracket.
 * Virtual rendering - schematics, ponder - has no Flywheel behind it, so there the shaft's own model
 * is used instead.
 */
public class BracketedKineticBlockModel extends DelegateBlockStateModel {

	public BracketedKineticBlockModel(BlockStateModel template) {
		super(template);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		ModelData data = level.getModelData(pos);
		if (VirtualRenderHelper.isVirtual(data)) {
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
