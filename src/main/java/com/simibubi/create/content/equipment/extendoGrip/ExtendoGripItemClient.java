package com.simibubi.create.content.equipment.extendoGrip;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The extendo grip's client half: reaching further than vanilla lets you, and telling the server so.
 * <p>
 * These three handlers sat on {@link ExtendoGripItem} and only ever ran on a client, which was fine
 * while {@code @OnlyIn(Dist.CLIENT)} stripped them. It does not in 26.2, and a {@link LocalPlayer}
 * local in a method body is resolved when the JVM verifies the class -- so the item could not be
 * loaded on a dedicated server at all, and Create failed to register. Splitting them out leaves the
 * item common and puts the client types behind a class that only a client loads.
 */
@EventBusSubscriber(Dist.CLIENT)
public class ExtendoGripItemClient {

	@SubscribeEvent
	public static void dontMissEntitiesWhenYouHaveHighReachDistance(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (mc.level == null || player == null)
			return;
		if (!ExtendoGripItem.isHoldingExtendoGrip(player))
			return;
		if (mc.hitResult instanceof BlockHitResult && mc.hitResult.getType() != Type.MISS)
			return;

		// Modified version of GameRenderer#getMouseOver
		double d0 = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
		if (!player.isCreative())
			d0 -= 0.5f;
		Vec3 Vector3d = player.getEyePosition(AnimationTickHolder.getPartialTicks());
		Vec3 Vector3d1 = player.getViewVector(1.0F);
		Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * d0, Vector3d1.y * d0, Vector3d1.z * d0);
		AABB AABB = player.getBoundingBox()
			.expandTowards(Vector3d1.scale(d0))
			.inflate(1.0D, 1.0D, 1.0D);
		EntityHitResult entityraytraceresult =
			ProjectileUtil.getEntityHitResult(player, Vector3d, Vector3d2, AABB, (e) -> {
				return !e.isSpectator() && e.isPickable();
			}, d0 * d0);
		if (entityraytraceresult != null) {
			Entity entity1 = entityraytraceresult.getEntity();
			Vec3 Vector3d3 = entityraytraceresult.getLocation();
			double d2 = Vector3d.distanceToSqr(Vector3d3);
			if (d2 < d0 * d0 || mc.hitResult == null || mc.hitResult.getType() == Type.MISS) {
				mc.hitResult = entityraytraceresult;
				if (entity1 instanceof LivingEntity || entity1 instanceof ItemFrame)
					mc.crosshairPickEntity = entity1;
			}
		}
	}

	@SubscribeEvent
	public static void notifyServerOfLongRangeAttacks(AttackEntityEvent event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if (!ExtendoGripItem.isUncaughtClientInteraction(entity, target))
			return;
		Player player = (Player) entity;
		if (ExtendoGripItem.isHoldingExtendoGrip(player))
			ClientNetworkHelper.INSTANCE.sendToServer(new ExtendoGripInteractionPacket(target));
	}

	@SubscribeEvent
	public static void notifyServerOfLongRangeInteractions(PlayerInteractEvent.EntityInteract event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if (!ExtendoGripItem.isUncaughtClientInteraction(entity, target))
			return;
		Player player = (Player) entity;
		if (ExtendoGripItem.isHoldingExtendoGrip(player))
			ClientNetworkHelper.INSTANCE
				.sendToServer(new ExtendoGripInteractionPacket(target, event.getHand(), event.getLocation()));
	}

	private ExtendoGripItemClient() {}
}
