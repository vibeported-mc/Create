package com.simibubi.create.content.equipment.zapper.terrainzapper;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.api.lang.Lang;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Where a worldshaper's brush is anchored relative to the block it is aimed at.
 * <p>
 * Sent over the network, so it is loaded on a dedicated server. Its button's {@code AllIcons} lives
 * in {@link TerrainZapperIcons}, because naming a GUI class from an enum constant makes the server
 * load one.
 */
public enum PlacementOptions implements StringRepresentable {
	Merged,
	Attached,
	Inserted;

	public static final Codec<PlacementOptions> CODEC = StringRepresentable.fromValues(PlacementOptions::values);
	public static final StreamCodec<ByteBuf, PlacementOptions> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PlacementOptions.class);

	public final String translationKey;

	PlacementOptions() {
		this.translationKey = Lang.asId(name());
	}

	@Override
	public @NotNull String getSerializedName() {
		return Lang.asId(name());
	}
}
