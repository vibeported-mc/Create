package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FilteringRenderer {
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		HitResult target = mc.hitResult;
		if (!(target instanceof BlockHitResult result))
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (mc.player.isShiftKeyDown())
			return;
		if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity sbe))
			return;

		ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

		for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
			if (!(b instanceof FilteringBehaviour behaviour))
				continue;

			if (behaviour instanceof SidedFilteringBehaviour sidedFilteringBehaviour) {
				behaviour = sidedFilteringBehaviour.get(result.getDirection());
				if (behaviour == null)
					continue;
			}

			if (!behaviour.isActive())
				continue;
			if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided)
				((Sided) behaviour.slotPositioning).fromSide(result.getDirection());
			if (!behaviour.slotPositioning.shouldRender(world, pos, state))
				continue;
			if (!behaviour.mayInteract(mc.player))
				continue;

			ItemStack filter = behaviour.getFilter();
			boolean isFilterSlotted = filter.getItem() instanceof FilterItem;
			boolean showCount = behaviour.isCountVisible();
			Component label = behaviour.getLabel();
			boolean hit = behaviour.slotPositioning.testHit(world, pos, state, target.getLocation()
				.subtract(Vec3.atLowerCornerOf(pos)));

			AABB emptyBB = new AABB(Vec3.ZERO, Vec3.ZERO);
			AABB bb = isFilterSlotted ? emptyBB.inflate(.45f, .31f, .2f) : emptyBB.inflate(.25f);

			ValueBox box = new ItemValueBox(label, bb, pos, filter, behaviour.getCountLabelForValueBox());
			box.passive(!hit || behaviour.bypassesInput(mainhandItem));

			Outliner.getInstance()
				.showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(behaviour.slotPositioning))
				.lineWidth(1 / 64f)
				.withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null)
				.highlightFace(result.getDirection());

			if (!hit)
				continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(behaviour.getTip());
			if (showCount)
				tip.add(behaviour.getAmountTip());

			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	/**
	 * Extract phase: walk the block entity's filtering behaviours and resolve everything needed to draw
	 * them, so that submission never has to touch the block entity again.
	 */
	@Nullable
	public static FilterRenderState getFilterRenderState(SmartBlockEntity be, ItemModelResolver itemModelResolver,
		Vec3 cameraPosition) {
		if (be == null || be.isRemoved())
			return null;

		Level level = be.getLevel();
		BlockPos blockPos = be.getBlockPos();
		BlockState blockState = be.getBlockState();
		List<FilterRenderState> states = new ArrayList<>();

		for (BlockEntityBehaviour b : be.getAllBehaviours()) {
			if (!(b instanceof FilteringBehaviour behaviour))
				continue;

			if (!be.isVirtual()) {
				float max = behaviour.getRenderDistance();
				if (cameraPosition.distanceToSqr(VecHelper.getCenterOf(blockPos)) > (max * max))
					continue;
			}

			if (!behaviour.isActive())
				continue;
			if (behaviour.getFilter().isEmpty() && !(behaviour instanceof SidedFilteringBehaviour))
				continue;

			ValueBoxTransform slotPositioning = behaviour.slotPositioning;

			if (slotPositioning instanceof Sided sided) {
				Direction previous = sided.getSide();
				for (Direction d : Iterate.directions) {
					ItemStack filter = behaviour.getFilter(d);
					if (filter.isEmpty())
						continue;

					sided.fromSide(d);
					if (!slotPositioning.shouldRender(level, blockPos, blockState))
						continue;

					// CONTRAPTION_CONTROLS draws its filter flat against the face rather than as a
					// floating item, which needs the GUI display context instead of FIXED.
					boolean flat = AllBlocks.CONTRAPTION_CONTROLS.has(blockState);
					states.add(SingleFilterRenderState.create(sided, d, level, blockPos, itemModelResolver, filter,
						flat));
				}
				sided.fromSide(previous);
			} else if (slotPositioning.shouldRender(level, blockPos, blockState)) {
				states.add(SingleFilterRenderState.create(slotPositioning, null, level, blockPos, itemModelResolver,
					behaviour.getFilter(), false));
			}
		}

		return states.isEmpty() ? null : new FilterRenderStates(states);
	}

	public interface FilterRenderState {
		void submit(BlockState blockState, SubmitNodeCollector queue, PoseStack ms, int light);
	}

	private record FilterRenderStates(List<FilterRenderState> states) implements FilterRenderState {
		@Override
		public void submit(BlockState blockState, SubmitNodeCollector queue, PoseStack ms, int light) {
			for (FilterRenderState state : states)
				state.submit(blockState, queue, ms, light);
		}
	}

	/**
	 * The level and position are held because Create's {@link ValueBoxTransform} resolves against them.
	 * Both are stable for the lifetime of a block entity, unlike the per-frame data that must be copied.
	 */
	public record SingleFilterRenderState(ValueBoxTransform slotPositioning, @Nullable Direction side, Level level,
		BlockPos pos, ItemStackRenderState item, boolean flat, float zOffset) implements FilterRenderState {

		static SingleFilterRenderState create(ValueBoxTransform slotPositioning, @Nullable Direction side, Level level,
			BlockPos pos, ItemModelResolver itemModelResolver, ItemStack filter, boolean flat) {
			ItemStackRenderState item = new ItemStackRenderState();
			ItemDisplayContext context = flat ? ItemDisplayContext.GUI : ItemDisplayContext.FIXED;
			item.displayContext = context;
			itemModelResolver.appendItemLayers(item, filter, context, level, null, 0);
			return new SingleFilterRenderState(slotPositioning, side, level, pos, item, flat,
				flat ? 0 : ValueBoxRenderer.customZOffset(filter.getItem()));
		}

		@Override
		public void submit(BlockState blockState, SubmitNodeCollector queue, PoseStack ms, int light) {
			ms.pushPose();
			if (side != null && slotPositioning instanceof Sided sided)
				sided.fromSide(side);
			slotPositioning.transform(level, pos, blockState, ms);
			if (flat)
				ValueBoxRenderer.renderFlatItemIntoValueBox(item, queue, ms, light);
			else
				ValueBoxRenderer.renderItemIntoValueBox(item, queue, ms, light, zOffset);
			ms.popPose();
		}
	}
}
