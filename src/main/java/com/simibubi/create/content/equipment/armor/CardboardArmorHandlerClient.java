package com.simibubi.create.content.equipment.armor;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import com.simibubi.create.foundation.utility.TickBasedCache;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class CardboardArmorHandlerClient {

	private static final Cache<UUID, Integer> BOXES_PLAYERS_ARE_HIDING_AS = new TickBasedCache<>(20, true);

	@SubscribeEvent
	public static void keepCacheAliveDesignDespiteNotRendering(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!CardboardArmorHandler.testForStealth(player))
			return;
		try {
			getCurrentBoxIndex(player);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Draws a sneaking player in cardboard armour as a package instead of as themselves.
	 * <p>
	 * The event carries the player's render state in 26.2 rather than the player, so everything the
	 * box needs - the entity to nudge by, its offset, its yaw - comes from there.
	 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void playerRendersAsBoxWhenSneaking(RenderPlayerEvent.Pre<?> event) {
		Minecraft mc = Minecraft.getInstance();
		AvatarRenderState renderState = event.getRenderState();
		Player player = mc.level == null ? null : mc.level.getEntity(renderState.id) instanceof Player p ? p : null;
		if (player == null || !CardboardArmorHandler.testForStealth(player))
			return;

		event.setCanceled(true);

		if (player == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON)
			return;

		PoseStack ms = event.getPoseStack();
		ms.pushPose();

		Vec3 renderOffset = event.getRenderer()
			.getRenderOffset(renderState);
		ms.translate(0, -renderOffset.y, 0);

		float movement = (float) player.position()
			.subtract(player.xo, player.yo, player.zo)
			.length();

		if (player.onGround())
			ms.translate(0,
				Math.min(Math.abs(Mth.cos((AnimationTickHolder.getRenderTime() % 256) / 2.0f)) * -renderOffset.y,
					movement * 5),
				0);

		float interpolatedYaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());

		float scale = player.getScale();
		ms.scale(scale, scale, scale);

		try {
			PartialModel model = AllPartialModels.PACKAGES_TO_HIDE_AS.get(getCurrentBoxIndex(player));
			SuperByteBufferRenderState box =
				PackageRenderer.extractBox(player, interpolatedYaw, renderState.lightCoords, model);
			if (box != null)
				box.submit(ms, RenderTypes.solidMovingBlock(), event.getSubmitNodeCollector());
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		ms.popPose();
	}

	private static Integer getCurrentBoxIndex(Player player) throws ExecutionException {
		return BOXES_PLAYERS_ARE_HIDING_AS.get(player.getUUID(),
			() -> player.level()
				.getRandom()
				.nextInt(AllPartialModels.PACKAGES_TO_HIDE_AS.size()));
	}

}
