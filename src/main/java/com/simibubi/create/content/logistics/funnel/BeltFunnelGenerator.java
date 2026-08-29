package com.simibubi.create.content.logistics.funnel;

import java.util.Optional;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BeltFunnelGenerator extends SpecialBlockStateGen {

	private static final TextureSlot BLOCK = TextureSlot.create("block");
	private static final TextureSlot DIRECTION = TextureSlot.create("direction");
	private static final TextureSlot REDSTONE = TextureSlot.create("redstone");
	private static final TextureSlot BASE = TextureSlot.create("base");

	private String type;
	private Identifier materialBlockTexture;

	public BeltFunnelGenerator(String type) {
		this.type = type;
		this.materialBlockTexture = Create.asResource("block/" + type + "_block");
	}

	@Override
	protected int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(BeltFunnelBlock.HORIZONTAL_FACING)) + 180;
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		String prefix = "block/funnel/";
		Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
		String shapeName = shape.getSerializedName();
		boolean powered = state.getOptionalValue(BlockStateProperties.POWERED)
			.orElse(false);
		String poweredSuffix = powered ? "_powered" : "_unpowered";
		String shapeSuffix = shape == Shape.PULLING ? "_pull" : shape == Shape.PUSHING ? "_push" : "_neutral";
		String name = ctx.getName() + "_" + shapeName + poweredSuffix;

		ModelTemplate template =
			new ModelTemplate(Optional.of(prov.modLoc("block/belt_funnel/block_" + shapeName)), Optional.empty(),
				TextureSlot.PARTICLE, BLOCK, DIRECTION, REDSTONE, BASE);
		return BlockModelGenerators.plainVariant(template.create(prov.modLoc("block/" + name), new TextureMapping()
			.put(TextureSlot.PARTICLE, new Material(materialBlockTexture))
			.put(BLOCK, new Material(materialBlockTexture))
			.put(DIRECTION, new Material(prov.modLoc(prefix + type + "_funnel" + shapeSuffix)))
			.put(REDSTONE, new Material(prov.modLoc(prefix + type + "_funnel" + poweredSuffix)))
			.put(BASE, new Material(prov.modLoc(prefix + type + "_funnel"))), prov.modelOutput));
	}

}
