package com.simibubi.create.content.fluids;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.TransformedModelPart;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * Adds a pipe's rim attachments, bracket and casing to its model.
 * <p>
 * Minecraft 26.2 collects a model into parts, so the extra pieces are simply more parts rather than
 * quads appended to the pipe's own. Which pieces to add is worked out here instead of travelling as
 * model data, and there is no render-type set to declare - the layer is a property of each quad.
 */
public class PipeAttachmentModel extends DelegateBlockStateModel {

	private final boolean ao;

	public static PipeAttachmentModel withAO(BlockStateModel template) {
		return new PipeAttachmentModel(template, true);
	}

	public static PipeAttachmentModel withoutAO(BlockStateModel template) {
		return new PipeAttachmentModel(template, false);
	}

	public PipeAttachmentModel(BlockStateModel template, boolean ao) {
		super(template);
		this.ao = ao;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		int from = parts.size();
		super.collectParts(level, pos, state, random, parts);

		PipeModelData pipeData = gatherPipeData(level, pos, state);

		BlockStateModel bracket = pipeData.getBracket();
		if (bracket != null)
			bracket.collectParts(level, pos, state, random, parts);

		for (Direction d : Iterate.directions) {
			AttachmentTypes type = pipeData.getAttachment(d);
			for (ComponentPartials partial : type.partials)
				AllPartialModels.PIPE_ATTACHMENTS.get(partial)
					.get(d)
					.get()
					.collectParts(level, pos, state, random, parts);
		}

		if (pipeData.isEncased())
			AllPartialModels.FLUID_PIPE_CASING.get()
				.collectParts(level, pos, state, random, parts);

		TransformedModelPart.forceAmbientOcclusion(parts, from, ao ? TriState.TRUE : TriState.FALSE);
	}

	private PipeModelData gatherPipeData(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		PipeModelData data = new PipeModelData();
		FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
		BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);

		if (transport != null)
			for (Direction d : Iterate.directions)
				data.putAttachment(d, transport.getRenderedRimAttachment(world, pos, state, d));
		if (bracket != null)
			data.putBracket(bracket.getBracket());

		data.setEncased(FluidPipeBlock.shouldDrawCasing(world, pos, state));
		return data;
	}

	private static class PipeModelData {
		private final AttachmentTypes[] attachments;
		private boolean encased;
		private @Nullable BlockStateModel bracket;

		public PipeModelData() {
			attachments = new AttachmentTypes[6];
			Arrays.fill(attachments, AttachmentTypes.NONE);
		}

		public void putBracket(BlockState state) {
			if (state != null) {
				this.bracket = Minecraft.getInstance()
					.getModelManager()
					.getBlockStateModelSet()
					.get(state);
			}
		}

		public @Nullable BlockStateModel getBracket() {
			return bracket;
		}

		public void putAttachment(Direction face, AttachmentTypes rim) {
			attachments[face.get3DDataValue()] = rim;
		}

		public AttachmentTypes getAttachment(Direction face) {
			return attachments[face.get3DDataValue()];
		}

		public void setEncased(boolean encased) {
			this.encased = encased;
		}

		public boolean isEncased() {
			return encased;
		}
	}

}
