package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class NetheriteBacktankFirstPersonRenderer {

	private static final Identifier BACKTANK_ARMOR_LOCATION =
		Create.asResource("textures/models/armor/netherite_diving_arm.png");

	private static boolean rendererActive = false;

	public static void clientTick() {
		Minecraft mc = Minecraft.getInstance();
		rendererActive =
			mc.player != null && AllItems.NETHERITE_BACKTANK.isIn(mc.player.getItemBySlot(EquipmentSlot.CHEST));
	}

	/**
	 * Draws the diving suit's sleeve over the first-person arm.
	 * <p>
	 * 26.2 poses models from a render state rather than from the entity, so the sleeve is reset the
	 * way vanilla's own first-person hand is instead of being animated from the player.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderPlayerHand(RenderArmEvent<?> event) {
		if (!rendererActive)
			return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null)
			return;

		AvatarRenderer<?> renderer = mc.getEntityRenderDispatcher()
			.getPlayerRenderer(player);
		PlayerModel model = renderer.getModel();
		ModelPart armPart = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
		armPart.resetPose();
		armPart.visible = true;
		armPart.xRot = 0.0F;

		event.getSubmitNodeCollector()
			.submitModelPart(armPart, event.getPoseStack(), RenderTypes.entitySolid(BACKTANK_ARMOR_LOCATION),
				LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
		event.setCanceled(true);
	}

}
