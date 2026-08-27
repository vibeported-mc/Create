package com.simibubi.create.content.equipment.armor;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderPlayerHand(RenderArmEvent event) {
		if (!rendererActive)
			return;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		MultiBufferSource buffer = event.getMultiBufferSource();
		if (!(mc.getEntityRenderDispatcher()
			.getRenderer(player) instanceof PlayerRenderer pr))
			return;

		PlayerModel<AbstractClientPlayer> model = pr.getModel();
		model.attackTime = 0.0F;
		model.crouching = false;
		model.swimAmount = 0.0F;
		model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		ModelPart armPart = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
		armPart.xRot = 0.0F;
		armPart.render(event.getPoseStack(), buffer.getBuffer(RenderTypes.entitySolid(BACKTANK_ARMOR_LOCATION)),
			LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		event.setCanceled(true);
	}

}
