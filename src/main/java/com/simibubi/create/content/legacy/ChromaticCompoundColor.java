package com.simibubi.create.content.legacy;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.Create;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The shifting colours of chromatic compound.
 * <p>
 * Minecraft 26.2 made item tinting data-driven: an item's model names the tint sources for its
 * layers, so this is a source registered under an id rather than a colour handler bound to an item.
 * Which layer is being tinted is now the model's business, so the layer travels in the source.
 */
public record ChromaticCompoundColor(int layer) implements ItemTintSource {

	public static final Identifier ID = Create.asResource("chromatic_compound");

	public static final MapCodec<ChromaticCompoundColor> MAP_CODEC = com.mojang.serialization.Codec.INT
		.fieldOf("layer")
		.xmap(ChromaticCompoundColor::new, ChromaticCompoundColor::layer);

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return 0;

		float pt = AnimationTickHolder.getPartialTicks();
		float progress =
			(float) ((mc.player.getViewYRot(pt)) / 180 * Math.PI) + (AnimationTickHolder.getRenderTime() / 10f);

		if (layer == 0)
			return Color.mixColors(ARGB.color(110, 87, 115), ARGB.color(107, 48, 116), (Mth.sin(progress) + 1) / 2);
		if (layer == 1)
			return Color.mixColors(ARGB.color(212, 93, 121), ARGB.color(110, 87, 115),
				(Mth.sin((float) (progress + Math.PI)) + 1) / 2);
		if (layer == 2)
			return Color.mixColors(ARGB.color(234, 144, 133), ARGB.color(212, 93, 121),
				(Mth.sin((float) (progress * 1.5f + Math.PI)) + 1) / 2);
		return 0;
	}

	@Override
	public MapCodec<ChromaticCompoundColor> type() {
		return MAP_CODEC;
	}

	public static void register(RegisterColorHandlersEvent.ItemTintSources event) {
		event.register(ID, MAP_CODEC);
	}

}
