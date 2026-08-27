package com.simibubi.create.compat.trainmap;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Hooks into the minimap mods Create draws train positions on.
 * <p>
 * FTB Chunks, JourneyMap and Xaero have no 26.2 builds yet, so the adapters they need are compiled
 * out and there is nothing left to hook. Create's own train map is unaffected.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class TrainMapEvents {

}
