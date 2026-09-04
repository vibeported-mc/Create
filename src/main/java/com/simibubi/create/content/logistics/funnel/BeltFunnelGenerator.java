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

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot BLOCK = TextureSlot.create("block");
		static final TextureSlot DIRECTION = TextureSlot.create("direction");
		static final TextureSlot REDSTONE = TextureSlot.create("redstone");
		static final TextureSlot BASE = TextureSlot.create("base");
	}


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
				TextureSlot.PARTICLE, Client.BLOCK, Client.DIRECTION, Client.REDSTONE, Client.BASE);
		return BlockModelGenerators.plainVariant(template.create(prov.modLoc("block/" + name), new TextureMapping()
			.put(TextureSlot.PARTICLE, new Material(materialBlockTexture))
			.put(Client.BLOCK, new Material(materialBlockTexture))
			.put(Client.DIRECTION, new Material(prov.modLoc(prefix + type + "_funnel" + shapeSuffix)))
			.put(Client.REDSTONE, new Material(prov.modLoc(prefix + type + "_funnel" + poweredSuffix)))
			.put(Client.BASE, new Material(prov.modLoc(prefix + type + "_funnel"))), prov.modelOutput));
	}

}
