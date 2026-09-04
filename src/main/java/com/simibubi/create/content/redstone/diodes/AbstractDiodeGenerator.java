package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

public abstract class AbstractDiodeGenerator extends SpecialBlockStateGen {

	/**
	 * The client-only constants this class builds its models from.
	 *
	 * A nested class is initialised on first use rather than with its owner, which is the whole
	 * point of the indirection: these are client types, this class is reached from common
	 * registration code, and a static field here would be initialised on a dedicated server that
	 * has no such class. Only the datagen methods below touch them, and a server runs none.
	 */
	private static final class Client {
		static final TextureSlot TOP = TextureSlot.create("top");
	}

	private List<MultiVariant> models;


	public static <I extends BlockItem> void diodeItemModel(DataGenContext<Item, I> c, RegistrateItemModelGenerator p) {
		String name = c.getName();
		String path = "block/diodes/";
		ModelTemplate template = new ModelTemplate(Optional.of(p.modLoc(path + name)), Optional.empty(), Client.TOP);
		p.generateWithTemplate(c.getEntry(), template,
			new TextureMapping().put(Client.TOP, new Material(p.modLoc(path + name + "/item"))));
	}

	@Override
	protected final int getXRotation(BlockState state) {
		return 0;
	}

	@Override
	protected final int getYRotation(BlockState state) {
		return horizontalAngle(state.getValue(AbstractDiodeBlock.FACING));
	}

	protected abstract <T extends Block> List<MultiVariant> createModels(DataGenContext<Block, T> ctx,
		RegistrateBlockModelGenerator prov);

	protected abstract int getModelIndex(BlockState state);

	@Override
	public final <T extends Block> MultiVariant getModel(DataGenContext<Block, T> ctx, RegistrateBlockModelGenerator prov,
		BlockState state) {
		if (models == null)
			models = createModels(ctx, prov);
		return models.get(getModelIndex(state));
	}

	protected MultiVariant existingModel(RegistrateBlockModelGenerator prov, String name) {
		return BlockModelGenerators.plainVariant(existing(name));
	}

	protected Identifier existing(String name) {
		return Create.asResource("block/diodes/" + name);
	}

	protected <T extends Block> Identifier texture(DataGenContext<Block, T> ctx, String name) {
		return Create.asResource("block/diodes/" + ctx.getName() + "/" + name);
	}

	protected Identifier poweredTorch() {
		return Identifier.withDefaultNamespace("block/redstone_torch");
	}

}
