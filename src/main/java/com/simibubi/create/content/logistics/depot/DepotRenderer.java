package com.simibubi.create.content.logistics.depot;

import net.neoforged.neoforge.transfer.item.ItemUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DepotRenderer extends SafeBlockEntityRenderer<DepotBlockEntity, DepotRenderer.DepotRenderState> {

	public static class DepotRenderState extends SafeRenderState {
		public final List<IncomingItemState> incoming = new ArrayList<>();
		public final List<OutputItemState> outputs = new ArrayList<>();
		public Vec3 itemPosition = Vec3.ZERO;
	}

	/**
	 * Everything a single in-world item needs once the block entity is out of reach. The three flags
	 * were all read off the ItemStack or its baked model, so they have to be resolved during
	 * extraction along with the model itself.
	 */
	public record ItemState(ItemStackRenderState item, boolean blockItem, boolean upright, boolean box, int count) {
		public static ItemState create(ItemModelResolver itemModelResolver, ItemStack stack, @Nullable Level level) {
			ItemStackRenderState item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, level, null, 0);
			return new ItemState(item, item.usesBlockLight(), BeltHelper.isItemUpright(stack),
				PackageItem.isPackage(stack), Mth.log2(stack.getCount()) / 2);
		}
	}

	public record IncomingItemState(ItemState item, int angle, @Nullable Vec3 offset, float sideOffset,
		boolean alongX) {
	}

	public record OutputItemState(ItemState item, int index, int angle, boolean upright) {
	}

	protected final ItemModelResolver itemModelResolver;

	public DepotRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public DepotRenderState createRenderState() {
		return new DepotRenderState();
	}

	@Override
	protected void extractSafe(DepotBlockEntity be, DepotRenderState state, float partialTicks, Vec3 cameraPosition) {
		extractItemsOf(be, state, partialTicks, be.depotBehaviour, itemModelResolver);
	}

	@Override
	protected void submitSafe(DepotRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		submitItemsOf(state, ms, queue, state.lightCoords);
	}

	public static void extractItemsOf(SmartBlockEntity be, DepotRenderState state, float partialTicks,
		DepotBehaviour depotBehaviour, ItemModelResolver itemModelResolver) {
		state.incoming.clear();
		state.outputs.clear();
		state.itemPosition = VecHelper.getCenterOf(be.getBlockPos());
		Level level = be.getLevel();

		TransportedItemStack transported = depotBehaviour.heldItem;
		if (transported != null)
			depotBehaviour.incoming.add(transported);

		for (TransportedItemStack tis : depotBehaviour.incoming) {
			float offset = Mth.lerp(partialTicks, tis.prevBeltPosition, tis.beltPosition);
			float sideOffset = Mth.lerp(partialTicks, tis.prevSideOffset, tis.sideOffset);

			Vec3 offsetVec = null;
			boolean alongX = false;
			if (tis.insertedFrom.getAxis()
				.isHorizontal()) {
				offsetVec = Vec3.atLowerCornerOf(tis.insertedFrom.getOpposite()
					.getUnitVec3i())
					.scale(.5f - offset);
				alongX = tis.insertedFrom.getClockWise()
					.getAxis() == Direction.Axis.X;
				if (!alongX)
					sideOffset *= -1;
			}

			state.incoming.add(new IncomingItemState(ItemState.create(itemModelResolver, tis.stack, level), tis.angle,
				offsetVec, sideOffset, alongX));
		}

		if (transported != null)
			depotBehaviour.incoming.remove(transported);

		for (int i = 0; i < depotBehaviour.processingOutputBuffer.size(); i++) {
			ItemStack stack = ItemUtil.getStack(depotBehaviour.processingOutputBuffer, i);
			if (stack.isEmpty())
				continue;
			boolean renderUpright = BeltHelper.isItemUpright(stack);
			Random r = new Random(i + 1);
			int angle = (int) (360 * r.nextFloat());
			state.outputs.add(new OutputItemState(ItemState.create(itemModelResolver, stack, level), i,
				renderUpright ? angle + 90 : angle, renderUpright));
		}
	}

	public static void submitItemsOf(DepotRenderState state, PoseStack ms, SubmitNodeCollector queue, int light) {
		if (state.incoming.isEmpty() && state.outputs.isEmpty())
			return;

		var msr = TransformStack.of(ms);

		ms.pushPose();
		ms.translate(.5f, 15 / 16f, .5f);

		// Render main items
		for (IncomingItemState incoming : state.incoming) {
			ms.pushPose();
			msr.nudge(0);
			Vec3 offsetVec = incoming.offset();
			if (offsetVec != null) {
				ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
				ms.translate(incoming.alongX() ? incoming.sideOffset() : 0, 0,
					incoming.alongX() ? 0 : incoming.sideOffset());
			}
			submitItem(ms, queue, light, incoming.item(), incoming.angle(), new Random(0), state.itemPosition, false);
			ms.popPose();
		}

		// Render output items
		for (OutputItemState output : state.outputs) {
			int i = output.index();
			ms.pushPose();
			msr.nudge(i);
			msr.rotateYDegrees(360 / 8f * i);
			ms.translate(.35f, 0, 0);
			if (output.upright())
				msr.rotateYDegrees(-(360 / 8f * i));
			submitItem(ms, queue, light, output.item(), output.angle(), new Random(i + 1), state.itemPosition, false);
			ms.popPose();
		}

		ms.popPose();
	}

	public static void submitItem(PoseStack ms, SubmitNodeCollector queue, int light, ItemState item, int angle,
		@Nullable Random r, Vec3 itemPosition, boolean alwaysUpright) {
		var msr = TransformStack.of(ms);
		int count = item.count();
		boolean blockItem = item.blockItem();
		boolean renderUpright = item.upright() || alwaysUpright && !blockItem;

		ms.pushPose();
		msr.rotateYDegrees(angle);

		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera()
				.position();
			Vec3 diff = itemPosition.subtract(cameraPosition);
			float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
			ms.mulPose(Axis.YP.rotation(yRot));
			ms.translate(0, 3 / 32d, -1 / 16f);
		}

		for (int i = 0; i <= count; i++) {
			ms.pushPose();
			if (blockItem && r != null)
				ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);

			if (item.box() && !alwaysUpright) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else if (blockItem && alwaysUpright) {
				ms.translate(0, 1 / 16f, 0);
				ms.scale(.755f, .755f, .755f);
			} else
				ms.scale(.5f, .5f, .5f);

			if (!blockItem && !renderUpright) {
				ms.translate(0, -3 / 16f, 0);
				msr.rotateXDegrees(90);
			}
			item.item()
				.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();

			if (!renderUpright) {
				if (!blockItem)
					msr.rotateYDegrees(10);
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);
		}

		ms.popPose();
	}

}
