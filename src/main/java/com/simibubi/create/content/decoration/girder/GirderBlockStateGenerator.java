package com.simibubi.create.content.decoration.girder;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;

public class GirderBlockStateGenerator {

	public static void blockStateWithShaft(DataGenContext<Block, GirderEncasedShaftBlock> c,
		RegistrateBlockModelGenerator p) {
		MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());

		builder.with(BlockModelGenerators.condition()
			.term(GirderEncasedShaftBlock.HORIZONTAL_AXIS, Axis.Z),
			BlockStateGen.rotateY(AssetLookup.partialBaseVariant(c, p), 0));

		builder.with(BlockModelGenerators.condition()
			.term(GirderEncasedShaftBlock.HORIZONTAL_AXIS, Axis.X),
			BlockStateGen.rotateY(AssetLookup.partialBaseVariant(c, p), 90));

		builder.with(BlockModelGenerators.condition()
			.term(GirderEncasedShaftBlock.TOP, true),
			AssetLookup.partialBaseVariant(c, p, "top"));

		builder.with(BlockModelGenerators.condition()
			.term(GirderEncasedShaftBlock.BOTTOM, true),
			AssetLookup.partialBaseVariant(c, p, "bottom"));

		p.blockStateOutput.accept(builder);
	}

	public static void blockState(DataGenContext<Block, GirderBlock> c, RegistrateBlockModelGenerator p) {
		MultiPartGenerator builder = MultiPartGenerator.multiPart(c.get());

		builder.with(BlockModelGenerators.condition()
			.term(GirderBlock.X, false)
			.term(GirderBlock.Z, false),
			AssetLookup.partialBaseVariant(c, p, "pole"));

		builder.with(BlockModelGenerators.condition()
			.term(GirderBlock.X, true),
			AssetLookup.partialBaseVariant(c, p, "x"));

		builder.with(BlockModelGenerators.condition()
			.term(GirderBlock.Z, true),
			AssetLookup.partialBaseVariant(c, p, "z"));

		for (boolean x : Iterate.trueAndFalse) {
			builder.with(BlockModelGenerators.condition()
				.term(GirderBlock.TOP, true)
				.term(GirderBlock.X, x)
				.term(GirderBlock.Z, !x),
				AssetLookup.partialBaseVariant(c, p, "top"));

			builder.with(BlockModelGenerators.condition()
				.term(GirderBlock.BOTTOM, true)
				.term(GirderBlock.X, x)
				.term(GirderBlock.Z, !x),
				AssetLookup.partialBaseVariant(c, p, "bottom"));
		}

		builder.with(BlockModelGenerators.condition()
			.term(GirderBlock.X, true)
			.term(GirderBlock.Z, true),
			AssetLookup.partialBaseVariant(c, p, "cross"));

		p.blockStateOutput.accept(builder);
	}

}
