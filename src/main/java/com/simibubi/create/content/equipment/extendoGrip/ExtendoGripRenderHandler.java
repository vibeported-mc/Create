package com.simibubi.create.content.equipment.extendoGrip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ExtendoGripRenderHandler {

	public static float mainHandAnimation;
	public static float lastMainHandAnimation;
	public static PartialModel pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;

	private static final ItemStackRenderState SCRATCH_ITEM = new ItemStackRenderState();

	public static void tick() {
		lastMainHandAnimation = mainHandAnimation;
		mainHandAnimation *= Mth.clamp(mainHandAnimation, 0.8f, 0.99f);

		pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;
		if (!AllItems.EXTENDO_GRIP.isIn(getRenderedOffHandStack()))
			return;
		ItemStack main = getRenderedMainHandStack();
		if (main.isEmpty())
			return;
		if (!(main.getItem() instanceof BlockItem))
			return;
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(SCRATCH_ITEM, main, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, null, null, 0);
		if (!SCRATCH_ITEM.usesBlockLight())
			return;
		pose = AllPartialModels.DEPLOYER_HAND_HOLDING;
	}

	@SubscribeEvent
	public static void onRenderPlayerHand(RenderHandEvent event) {
		ItemStack heldItem = event.getItemStack();
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;

		ItemStack offhandItem = getRenderedOffHandStack();
		boolean notInOffhand = !AllItems.EXTENDO_GRIP.isIn(offhandItem);
		if (notInOffhand && !AllItems.EXTENDO_GRIP.isIn(heldItem))
			return;

		PoseStack ms = event.getPoseStack();
		var msr = TransformStack.of(ms);

		float flip = rightHand ? 1.0F : -1.0F;
		float swingProgress = event.getSwingProgress();
		boolean blockItem = heldItem.getItem() instanceof BlockItem;
		float equipProgress = blockItem ? 0 : event.getEquipProgress() / 4;

		ms.pushPose();
		if (event.getHand() == InteractionHand.MAIN_HAND) {

			if (1 - swingProgress > mainHandAnimation && swingProgress > 0)
				mainHandAnimation = 0.95f;
			ms.translate(flip * (0.64000005F - .1f), -0.4F + equipProgress * -0.6F, -0.71999997F + .3f);

			ms.pushPose();
			msr.rotateYDegrees(flip * 75.0F);
			ms.translate(flip * -1.0F, 3.6F, 3.5F);
			msr.rotateZDegrees(flip * 120)
				.rotateXDegrees(200)
				.rotateYDegrees(flip * -135.0F);
			ms.translate(flip * 5.6F, 0.0F, 0.0F);
			msr.rotateYDegrees(flip * 40.0F);
			ms.translate(flip * 0.05f, -0.3f, -0.3f);

			AvatarRenderer<AbstractClientPlayer> playerrenderer = mc.getEntityRenderDispatcher()
				.getPlayerRenderer(player);
			Identifier skin = player.getSkin()
				.body()
				.texturePath();
			if (rightHand)
				playerrenderer.renderRightHand(ms, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
					player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
			else
				playerrenderer.renderLeftHand(ms, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
					player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
			ms.popPose();

			// Render gun
			ms.pushPose();
			ms.translate(flip * -0.1f, 0, -0.3f);
			ItemInHandRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
			ItemDisplayContext transform =
				rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			firstPersonRenderer.renderItem(mc.player, notInOffhand ? heldItem : offhandItem, transform, ms,
				event.getSubmitNodeCollector(), event.getPackedLight());

			ms.popPose();
		}
		ms.popPose();
		event.setCanceled(true);
	}

	private static ItemStack getRenderedMainHandStack() {
		return Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().mainHandItem;
	}

	private static ItemStack getRenderedOffHandStack() {
		return Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().offHandItem;
	}

}
