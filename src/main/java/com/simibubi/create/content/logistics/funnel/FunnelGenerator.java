package com.simibubi.create.content.logistics.funnel;

import java.util.Optional;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public class FunnelGenerator extends SpecialBlockStateGen {

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot BASE = TextureSlot.create("base");
		static final TextureSlot REDSTONE = TextureSlot.create("redstone");
		static final TextureSlot DIRECTION = TextureSlot.create("direction");
		static final TextureSlot BLOCK = TextureSlot.create("block");
		static final TextureSlot FRAME = TextureSlot.create("frame");
		static final TextureSlot OPEN = TextureSlot.create("open");
	}


	private String type;
	private Identifier blockTexture;
	private boolean hasFilter;

	public FunnelGenerator(String type, boolean hasFilter) {
		this.type = type;
		this.hasFilter = hasFilter;
		this.blockTexture = Create.asResource("block/" + type + "_block");
	}

	@Override
	protected int getXRotation(BlockState state) {
		return state.getValue(FunnelBlock.FACING) == Direction.DOWN ? 180 : 0;
	}

	@Override
	protected int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(FunnelBlock.FACING)) + 180;
	}

	@Override
	public <T extends Block> MultiVariant getModel(DataGenContext<Block, T> c, RegistrateBlockModelGenerator p,
		BlockState s) {
		String prefix = "block/funnel/";
		String powered = s.getValue(FunnelBlock.POWERED) ? "_powered" : "_unpowered";
		String closed = s.getValue(FunnelBlock.POWERED) ? "_closed" : "_open";
		String extracting = s.getValue(FunnelBlock.EXTRACTING) ? "_push" : "_pull";
		Direction facing = s.getValue(FunnelBlock.FACING);
		boolean horizontal = facing.getAxis()
			.isHorizontal();
		String parent = horizontal ? "horizontal" : hasFilter ? "vertical" : "vertical_filterless";

		TextureMapping textures = new TextureMapping()
			.put(TextureSlot.PARTICLE, new Material(blockTexture))
			.put(Client.BASE, new Material(p.modLoc(prefix + type + "_funnel")))
			.put(Client.REDSTONE, new Material(p.modLoc(prefix + type + "_funnel" + powered)))
			.put(Client.DIRECTION, new Material(p.modLoc(prefix + type + "_funnel" + extracting)));

		TextureSlot[] extra;
		if (horizontal) {
			textures.put(Client.BLOCK, new Material(blockTexture));
			extra = new TextureSlot[] { Client.BLOCK };
		} else {
			textures.put(Client.FRAME, new Material(p.modLoc(prefix + type + "_funnel_frame")));
			textures.put(Client.OPEN, new Material(p.modLoc(prefix + "funnel" + closed)));
			extra = new TextureSlot[] { Client.FRAME, Client.OPEN };
		}

		TextureSlot[] slots = new TextureSlot[4 + extra.length];
		slots[0] = TextureSlot.PARTICLE;
		slots[1] = Client.BASE;
		slots[2] = Client.REDSTONE;
		slots[3] = Client.DIRECTION;
		System.arraycopy(extra, 0, slots, 4, extra.length);

		ModelTemplate template =
			new ModelTemplate(Optional.of(p.modLoc(prefix + "block_" + parent)), Optional.empty(), slots);
		return BlockModelGenerators.plainVariant(template.create(
			p.modLoc("block/" + type + "_funnel_" + parent + extracting + powered), textures, p.modelOutput));
	}

	public static NonNullBiConsumer<DataGenContext<Item, FunnelItem>, RegistrateItemModelGenerator> itemModel(
		String type) {
		String prefix = "block/funnel/";
		Identifier blockTexture = Create.asResource("block/" + type + "_block");
		return (c, p) -> {
			ModelTemplate template = new ModelTemplate(Optional.of(p.modLoc("block/funnel/item")), Optional.empty(),
				TextureSlot.PARTICLE, Client.BLOCK, Client.BASE, Client.DIRECTION, Client.REDSTONE);
			p.generateWithTemplate(c.getEntry(), template, new TextureMapping()
				.put(TextureSlot.PARTICLE, new Material(blockTexture))
				.put(Client.BLOCK, new Material(blockTexture))
				.put(Client.BASE, new Material(p.modLoc(prefix + type + "_funnel")))
				.put(Client.DIRECTION, new Material(p.modLoc(prefix + type + "_funnel_neutral")))
				.put(Client.REDSTONE, new Material(p.modLoc(prefix + type + "_funnel_unpowered"))));
		};
	}

}
