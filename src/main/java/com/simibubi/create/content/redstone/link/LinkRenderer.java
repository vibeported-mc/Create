package com.simibubi.create.content.redstone.link;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LinkRenderer {

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		HitResult target = mc.hitResult;
		if (target == null || !(target instanceof BlockHitResult result))
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();

		LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		Component freq1 = CreateLang.translateDirect("logistics.firstFrequency");
		Component freq2 = CreateLang.translateDirect("logistics.secondFrequency");

		for (boolean first : Iterate.trueAndFalse) {
			AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);
			Component label = first ? freq1 : freq2;
			boolean hit = behaviour.testHit(first, target.getLocation());
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;

			ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
			boolean empty = behaviour.getNetworkKey()
				.get(first)
				.getStack()
				.isEmpty();

			if (!empty)
				box.wideOutline();

			Outliner.getInstance().showOutline(Pair.of(Boolean.valueOf(first), pos), box.transform(transform))
				.highlightFace(result.getDirection());

			if (!hit)
				continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(
				CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	/**
	 * Extract phase: resolve both frequency slots so submission never touches the block entity.
	 */
	@Nullable
	public static LinkRenderState getLinkRenderState(SmartBlockEntity be, ItemModelResolver itemModelResolver,
		Vec3 cameraPosition) {
		if (be == null || be.isRemoved())
			return null;

		float max = AllConfigs.client().filterItemRenderDistance.getF();
		if (!be.isVirtual() && cameraPosition.distanceToSqr(VecHelper.getCenterOf(be.getBlockPos())) > (max * max))
			return null;

		LinkBehaviour behaviour = be.getBehaviour(LinkBehaviour.TYPE);
		if (behaviour == null)
			return null;

		Level level = be.getLevel();
		BlockPos pos = be.getBlockPos();
		List<SlotRenderState> slots = new ArrayList<>(2);

		for (boolean first : Iterate.trueAndFalse) {
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;
			ItemStack stack = first ? behaviour.frequencyFirst.getStack() : behaviour.frequencyLast.getStack();

			ItemStackRenderState item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, level, null, 0);
			slots.add(new SlotRenderState(transform, level, pos, item,
				ValueBoxRenderer.customZOffset(stack.getItem())));
		}

		return new LinkRenderState(slots);
	}

	public record LinkRenderState(List<SlotRenderState> slots) {
		public void submit(BlockState blockState, SubmitNodeCollector queue, PoseStack ms, int light) {
			for (SlotRenderState slot : slots)
				slot.submit(blockState, queue, ms, light);
		}
	}

	/**
	 * The level and position are held because Create's {@link ValueBoxTransform} resolves against them;
	 * both are stable for the lifetime of the block entity.
	 */
	public record SlotRenderState(ValueBoxTransform transform, Level level, BlockPos pos, ItemStackRenderState item,
		float zOffset) {
		void submit(BlockState blockState, SubmitNodeCollector queue, PoseStack ms, int light) {
			ms.pushPose();
			transform.transform(level, pos, blockState, ms);
			ValueBoxRenderer.renderItemIntoValueBox(item, queue, ms, light, zOffset);
			ms.popPose();
		}
	}

}
