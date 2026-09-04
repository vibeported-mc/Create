package com.simibubi.create.foundation.utility;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The running client's level and player, as the common types.
 * <p>
 * Common code sometimes has to ask what the local player is looking at -- an item drawing its own
 * selection bounds, say. Writing {@code Minecraft.getInstance().level} inline puts {@link Minecraft}
 * in the body of a common method, and the JVM resolves that when it verifies the class, so the block
 * or item holding it cannot be loaded on a dedicated server at all.
 * <p>
 * Reached through here instead, the caller names only {@link Level} and {@link Player}, and the
 * client class is resolved when the call actually runs -- which is to say, only on a client. Until
 * 26.2 {@code @OnlyIn(Dist.CLIENT)} stripped such methods and the question did not arise.
 */
public class ClientAccess {

	@Nullable
	public static Level level() {
		return Minecraft.getInstance().level;
	}

	@Nullable
	public static Player player() {
		return Minecraft.getInstance().player;
	}

	private ClientAccess() {}
}
