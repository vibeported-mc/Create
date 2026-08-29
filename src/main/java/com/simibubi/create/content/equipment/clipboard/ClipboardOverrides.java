package com.simibubi.create.content.equipment.clipboard;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.simibubi.create.Create;

import java.util.ArrayList;
import java.util.List;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.Item;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.api.lang.Lang;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;


public class ClipboardOverrides {

	public enum ClipboardType implements StringRepresentable {
		EMPTY("empty_clipboard"), WRITTEN("clipboard"), EDITING("clipboard_and_quill");

		public static final Codec<ClipboardType> CODEC = StringRepresentable.fromValues(ClipboardType::values);
		public static final StreamCodec<ByteBuf, ClipboardType> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(ClipboardType.class);

		public final String file;
		public static Identifier ID = Create.asResource("clipboard_type");

		ClipboardType(String file) {
			this.file = file;
		}

		@Override
		public @NotNull String getSerializedName() {
			return Lang.asId(name());
		}
	}

	/**
	 * One flat model per clipboard type, picked by {@link ClipboardTypeProperty}. The empty clipboard
	 * is also the fallback, for a stack that carries no clipboard content at all.
	 */
	public static void addOverrideModels(DataGenContext<Item, ClipboardBlockItem> c,
		RegistrateItemModelGenerator p) {
		List<SelectItemModel.SwitchCase<ClipboardType>> cases = new ArrayList<>();
		Identifier fallback = null;

		for (ClipboardType type : ClipboardType.values()) {
			Identifier model = ModelTemplates.FLAT_ITEM.create(
				p.modLoc("item/" + c.getName() + "_" + type.ordinal()),
				TextureMapping.layer0(new Material(Create.asResource("item/" + type.file))), p.modelOutput);
			if (type == ClipboardType.EMPTY)
				fallback = model;
			cases.add(ItemModelUtils.when(type, ItemModelUtils.plainModel(model)));
		}

		p.itemModelOutput.accept(c.getEntry(), ItemModelUtils.select(new ClipboardTypeProperty(),
			ItemModelUtils.plainModel(fallback), cases));
	}

}
