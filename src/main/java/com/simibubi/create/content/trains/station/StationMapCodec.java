package com.simibubi.create.content.trains.station;

import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Carries Create's station markers along with a map's own saved data.
 * <p>
 * 26.2 saves map data through a codec rather than the {@code save}/{@code load} pair Create used to
 * append to, so the markers are written as one more field beside what vanilla writes, and read back
 * out of the same object.
 */
public class StationMapCodec {

	private static final String KEY = "create:stations";
	private static final Codec<List<StationMarker>> MARKERS = StationMarker.CODEC.listOf();

	public static Codec<MapItemSavedData> withStations(Codec<MapItemSavedData> base) {
		return new Codec<>() {

			@Override
			public <T> DataResult<Pair<MapItemSavedData, T>> decode(DynamicOps<T> ops, T input) {
				return base.decode(ops, input)
					.map(decoded -> {
						MapItemSavedData data = decoded.getFirst();
						ops.get(input, KEY)
							.flatMap(markers -> MARKERS.parse(ops, markers))
							.result()
							.ifPresent(markers -> markers
								.forEach(marker -> ((StationMapData) data).addStationMarker(marker)));
						return decoded;
					});
			}

			@Override
			public <T> DataResult<T> encode(MapItemSavedData input, DynamicOps<T> ops, T prefix) {
				return base.encode(input, ops, prefix)
					.flatMap(encoded -> {
						List<StationMarker> markers = ((StationMapData) input).create$getStationMarkers();
						if (markers.isEmpty())
							return DataResult.success(encoded);
						return MARKERS.encodeStart(ops, markers)
							.map(list -> ops.set(encoded, KEY, list));
					});
			}
		};
	}

	private StationMapCodec() {
	}

}
