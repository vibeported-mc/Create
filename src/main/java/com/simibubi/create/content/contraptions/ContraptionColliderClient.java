package com.simibubi.create.content.contraptions;

import java.util.WeakHashMap;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The part of {@link ContraptionCollider} that is about the player at the keyboard.
 * <p>
 * Keeping a rider from being pushed through the floor by the contraption they are standing on is a
 * client's business -- it is the client that owns its own player's position -- and these three name
 * client classes to do it. That was fine while {@code @OnlyIn(Dist.CLIENT)} stripped them. In 26.2
 * the bodies stay, and a {@link LocalPlayer} handed to something taking a {@code Player} is resolved
 * when the JVM verifies the class. So the collider could not be loaded on a dedicated server, and
 * the server crashed in its tick loop the moment any contraption existed -- out of
 * {@code ContraptionHandler.tick}, which is about as far from a rendering concern as it gets.
 * <p>
 * Reached through {@code executeOnClientOnly} and a static call, so only a client resolves any of
 * it.
 */
public class ContraptionColliderClient {

	private static int packetCooldown = 0;

	static void saveClientPlayerFromClipping(AbstractContraptionEntity contraptionEntity,
		Vec3 contraptionMotion) {
		LocalPlayer entity = Minecraft.getInstance().player;
		if (entity.isPassenger())
			return;

		double prevDiff = ContraptionCollider.safetyLock.right;
		double currentDiff = entity.getY() - contraptionEntity.getY();
		double motion = contraptionMotion.subtract(entity.getDeltaMovement()).y;
		double trend = Math.signum(currentDiff - prevDiff);

		ClientPacketListener handler = entity.connection;
		if (handler.getOnlinePlayers()
			.size() > 1) {
			if (packetCooldown > 0)
				packetCooldown--;
			if (packetCooldown == 0) {
				ClientNetworkHelper.INSTANCE.sendToServer(
					new ContraptionColliderLockPacketRequest(contraptionEntity.getId(), currentDiff));
				packetCooldown = 3;
			}
		}

		if (trend == 0)
			return;
		if (trend == Math.signum(motion))
			return;

		double speed = contraptionMotion.multiply(0, 1, 0)
			.lengthSqr();
		if (trend > 0 && speed < 0.1)
			return;
		if (speed < 0.05)
			return;

		if (!ContraptionCollider.savePlayerFromClipping(entity, contraptionEntity, contraptionMotion, prevDiff))
			ContraptionCollider.safetyLock.setLeft(null);
	}

	public static void lockPacketReceived(int contraptionId, int remotePlayerId, double suggestedOffset) {
		ClientLevel level = Minecraft.getInstance().level;
		if (!(level.getEntity(contraptionId) instanceof ControlledContraptionEntity contraptionEntity))
			return;
		if (!(level.getEntity(remotePlayerId) instanceof RemotePlayer player))
			return;
		ContraptionCollider.remoteSafetyLocks.computeIfAbsent(contraptionEntity, $ -> new WeakHashMap<>())
			.put(player, suggestedOffset);
	}

	static boolean isClientPlayerEntity(Entity entity) {
		return entity instanceof LocalPlayer;
	}

	private ContraptionColliderClient() {}
}
