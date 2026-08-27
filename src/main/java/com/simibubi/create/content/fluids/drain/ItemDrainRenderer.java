package com.simibubi.create.content.fluids.drain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.content.logistics.depot.DepotRenderer.ItemState;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class ItemDrainRenderer
	extends SmartBlockEntityRenderer<ItemDrainBlockEntity, ItemDrainRenderer.ItemDrainRenderState> {

	public static class ItemDrainRenderState extends SmartRenderState {
		public final List<FluidBox> fluids = new ArrayList<>();
		public @Nullable HeldItem item;
	}

	public record FluidBox(FluidStack fluid, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax,
		float verticalOffset, boolean renderBottom) {
	}

	/**
	 * The item rolling across the drain. Everything the transform needs is resolved during extraction;
	 * only the camera-facing turn for upright items is left to submit time.
	 */
	public record HeldItem(ItemState item, Direction insertedFrom, float offset, float sideOffset, boolean alongX,
		boolean upright, int copies, Vec3 itemPosition, Vec3 offsetVec) {
	}

	public ItemDrainRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ItemDrainRenderState createRenderState() {
		return new ItemDrainRenderState();
	}

	@Override
	protected void extractSafe(ItemDrainBlockEntity be, ItemDrainRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.fluids.clear();
		state.item = null;

		extractFluid(be, state, partialTicks);
		extractItem(be, state, partialTicks);
	}

	protected void extractItem(ItemDrainBlockEntity be, ItemDrainRenderState state, float partialTicks) {
		TransportedItemStack transported = be.heldItem;
		if (transported == null)
			return;

		Direction insertedFrom = transported.insertedFrom;
		if (!insertedFrom.getAxis()
			.isHorizontal())
			return;

		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

		Vec3 offsetVec = Vec3.atLowerCornerOf(insertedFrom.getOpposite()
			.getUnitVec3i())
			.scale(.5f - offset);
		boolean alongX = insertedFrom.getClockWise()
			.getAxis() == Direction.Axis.X;
		if (!alongX)
			sideOffset *= -1;

		ItemStack itemStack = transported.stack;
		state.item = new HeldItem(ItemState.create(itemModelResolver, itemStack, be.getLevel()), insertedFrom, offset,
			sideOffset, alongX, BeltHelper.isItemUpright(itemStack),
			(int) (Mth.log2((int) (itemStack.getCount()))) / 2, VecHelper.getCenterOf(be.getBlockPos()), offsetVec);
	}

	protected void extractFluid(ItemDrainBlockEntity be, ItemDrainRenderState state, float partialTicks) {
		SmartFluidTankBehaviour tank = be.internalTank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
			float yMin = 5f / 16f;
			float min = 2f / 16f;
			float max = min + (12 / 16f);
			float yOffset = (7 / 16f) * level;
			state.fluids.add(new FluidBox(fluidStack.copy(), min, yMin - yOffset, min, max, yMin, max, yOffset,
				false));
		}

		ItemStack heldItemStack = be.getHeldItemStack();
		if (heldItemStack.isEmpty())
			return;
		FluidStack draining = GenericItemEmptying.emptyItem(be.getLevel(), heldItemStack, true)
			.getFirst();
		if (draining.isEmpty()) {
			if (fluidStack.isEmpty())
				return;
			draining = fluidStack;
		}

		int processingTicks = be.processingTicks;
		float processingPT = be.processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = Mth.clamp(processingProgress, 0, 1);

		if (processingTicks != -1) {
			float radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			AABB bb = new AABB(0.5, 1.0, 0.5, 0.5, 0.25, 0.5).inflate(radius / 32f);
			state.fluids.add(new FluidBox(draining.copy(), (float) bb.minX, (float) bb.minY, (float) bb.minZ,
				(float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, 0, true));
		}
	}

	@Override
	protected void submitSafe(ItemDrainRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		for (FluidBox box : state.fluids) {
			ms.pushPose();
			ms.translate(0, box.verticalOffset(), 0);
			FluidRenderHelper.submitFluidBox(queue, box.fluid(), box.xMin(), box.yMin(), box.zMin(), box.xMax(),
				box.yMax(), box.zMax(), ms, state.lightCoords, box.renderBottom(), false);
			ms.popPose();
		}

		if (state.item != null)
			submitItem(state.item, ms, queue, state.lightCoords);
	}

	private static void submitItem(HeldItem held, PoseStack ms, SubmitNodeCollector queue, int light) {
		var msr = TransformStack.of(ms);
		Direction insertedFrom = held.insertedFrom();

		ms.pushPose();
		ms.translate(.5f, 15 / 16f, .5f);
		msr.nudge(0);

		Vec3 offsetVec = held.offsetVec();
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		ms.translate(held.alongX() ? held.sideOffset() : 0, 0, held.alongX() ? 0 : held.sideOffset());

		Random r = new Random(0);
		boolean renderUpright = held.upright();
		boolean blockItem = held.item()
			.blockItem();

		if (renderUpright)
			ms.translate(0, 3 / 32d, 0);

		int positive = insertedFrom.getAxisDirection()
			.getStep();
		float verticalAngle = positive * held.offset() * 360;
		if (insertedFrom.getAxis() != Direction.Axis.X)
			msr.rotateXDegrees(verticalAngle);
		if (insertedFrom.getAxis() != Direction.Axis.Z)
			msr.rotateZDegrees(-verticalAngle);

		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera()
				.getPosition();
			Vec3 vectorForOffset = held.itemPosition()
				.add(offsetVec);
			Vec3 diff = vectorForOffset.subtract(cameraPosition);

			if (insertedFrom.getAxis() != Direction.Axis.X)
				diff = VecHelper.rotate(diff, verticalAngle, Direction.Axis.X);
			if (insertedFrom.getAxis() != Direction.Axis.Z)
				diff = VecHelper.rotate(diff, -verticalAngle, Direction.Axis.Z);

			float yRot = (float) Mth.atan2(diff.z, -diff.x);
			ms.mulPose(Axis.YP.rotation((float) (yRot - Math.PI / 2)));
			ms.translate(0, 0, -1 / 16f);
		}

		for (int i = 0; i <= held.copies(); i++) {
			ms.pushPose();
			if (blockItem)
				ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);
			ms.scale(.5f, .5f, .5f);
			if (!blockItem && !renderUpright)
				msr.rotateXDegrees(90);
			held.item()
				.item()
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
