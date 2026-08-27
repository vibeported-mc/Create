package com.simibubi.create.content.trains.station;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public interface StationMapData {

	boolean toggleStation(LevelAccessor level, BlockPos pos, StationBlockEntity stationBlockEntity);

	void addStationMarker(StationMarker marker);

	/**
	 * The markers this map is carrying, for saving them alongside it.
	 */
	List<StationMarker> create$getStationMarkers();

}
