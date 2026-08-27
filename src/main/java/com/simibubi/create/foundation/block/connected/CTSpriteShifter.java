package com.simibubi.create.foundation.block.connected;

import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

public class CTSpriteShifter {

	private static final Map<String, CTSpriteShiftEntry> ENTRY_CACHE = new HashMap<>();

	public static CTSpriteShiftEntry getCT(CTType type, Identifier blockTexture, Identifier connectedTexture) {
		String key = blockTexture + "->" + connectedTexture + "+" + type.getId();
		if (ENTRY_CACHE.containsKey(key))
			return ENTRY_CACHE.get(key);

		CTSpriteShiftEntry entry = new CTSpriteShiftEntry(type);
		if (PlatformHelper.INSTANCE.getEnv().isClient())
			entry.set(blockTexture, connectedTexture);
		ENTRY_CACHE.put(key, entry);
		return entry;
	}

}
