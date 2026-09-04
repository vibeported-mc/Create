package com.simibubi.create.content.equipment.zapper;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllDataComponents;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.api.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * How a zapper spreads its blocks over the area it affects.
 * <p>
 * The pattern travels in a data component and over the network, so it is loaded wherever a zapper is
 * -- including on a dedicated server. It used to carry the {@link AllIcons} its button is drawn
 * with, which made its {@code <clinit>} load a GUI class; the icon lives in
 * {@link PlacementPatternsClient} instead.
 */
public enum PlacementPatterns implements StringRepresentable {
	Solid,
	Checkered,
	InverseCheckered,
	Chance25,
	Chance50,
	Chance75;

	public static final Codec<PlacementPatterns> CODEC = StringRepresentable.fromValues(PlacementPatterns::values);
	public static final StreamCodec<ByteBuf, PlacementPatterns> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(PlacementPatterns.class);

	public final String translationKey;

	private PlacementPatterns() {
		this.translationKey = Lang.asId(name());
	}

	public static void applyPattern(List<BlockPos> blocksIn, ItemStack stack, RandomSource random) {
		PlacementPatterns pattern = stack.getOrDefault(AllDataComponents.PLACEMENT_PATTERN, Solid);
		Predicate<BlockPos> filter = Predicates.alwaysFalse();

		switch (pattern) {
		case Chance25:
			filter = pos -> random.nextBoolean() || random.nextBoolean();
			break;
		case Chance50:
			filter = pos -> random.nextBoolean();
			break;
		case Chance75:
			filter = pos -> random.nextBoolean() && random.nextBoolean();
			break;
		case Checkered:
			filter = pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0;
			break;
		case InverseCheckered:
			filter = pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 != 0;
			break;
		case Solid:
		default:
			break;
		}

		blocksIn.removeIf(filter);
	}

	@Override
	public @NotNull String getSerializedName() {
		return Lang.asId(name());
	}
}
