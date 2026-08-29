package com.simibubi.create.content.equipment.clipboard;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;

/**
 * Which of the three clipboard models to draw.
 * <p>
 * This used to be an item property registered on the client, read back by numeric overrides on the
 * item model. Minecraft 26.2 has no overrides: an item model is a tree, and a "select" node picks a
 * branch by asking a named property for a value. So the property is a registered type of its own,
 * and the model names it.
 */
public record ClipboardTypeProperty() implements SelectItemModelProperty<ClipboardType> {

	public static final SelectItemModelProperty.Type<ClipboardTypeProperty, ClipboardType> TYPE =
		SelectItemModelProperty.Type.create(MapCodec.unit(new ClipboardTypeProperty()), ClipboardType.CODEC);

	@Override
	public @Nullable ClipboardType get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner,
		int seed, ItemDisplayContext displayContext) {
		return stack.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY)
			.type();
	}

	@Override
	public Codec<ClipboardType> valueCodec() {
		return ClipboardType.CODEC;
	}

	@Override
	public SelectItemModelProperty.Type<ClipboardTypeProperty, ClipboardType> type() {
		return TYPE;
	}

	public static void register(RegisterSelectItemModelPropertyEvent event) {
		event.register(ClipboardType.ID, TYPE);
	}

}
