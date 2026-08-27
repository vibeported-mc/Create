package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.item.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.EmptyItemHandler;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import com.simibubi.create.foundation.item.ModifiableItemHandler;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.data.IntAttached;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class BasinRenderer extends SmartBlockEntityRenderer<BasinBlockEntity, BasinRenderer.BasinRenderState> {

	public static class BasinRenderState extends SmartRenderState {
		public final List<FluidBox> fluids = new ArrayList<>();
		public final List<StackedItem> ingredients = new ArrayList<>();
		public final List<OutputItem> outputs = new ArrayList<>();
		public float fluidLevel;
		public float rotation;
		public float bobPhase;
		public int seed;
		public int itemCount;
		public Vec3 baseVector = Vec3.ZERO;
		public @Nullable Direction outputDirection;
		public boolean outToBasin;
	}

	public record FluidBox(FluidStack fluid, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax) {
	}

	/**
	 * One ingredient, drawn once per copy with a small random scatter.
	 */
	public record StackedItem(DepotRenderer.ItemState item, int copies) {
	}

	public record OutputItem(DepotRenderer.ItemState item, float progress) {
	}

	public BasinRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BasinRenderState createRenderState() {
		return new BasinRenderState();
	}

	@Override
	protected void extractSafe(BasinBlockEntity basin, BasinRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(basin, state, partialTicks, cameraPosition);
		state.fluids.clear();
		state.ingredients.clear();
		state.outputs.clear();
		state.outputDirection = null;

		float fluidLevel = extractFluids(basin, state, partialTicks);
		float level = Mth.clamp(fluidLevel - .3f, .125f, .6f);
		state.fluidLevel = fluidLevel;
		state.rotation = basin.ingredientRotation.getValue(partialTicks);
		state.seed = basin.getBlockPos()
			.hashCode();
		state.bobPhase = AnimationTickHolder.getRenderTime(basin.getLevel()) / 12f;

		ModifiableItemHandler inv = basin.itemCapability;
		if (inv == null)
			inv = EmptyItemHandler.INSTANCE;

		int itemCount = 0;
		for (int slot = 0; slot < inv.size(); slot++)
			if (!inv.getResource(slot)
				.isEmpty())
				itemCount++;

		state.baseVector = itemCount == 1 ? new Vec3(0, level, 0) : new Vec3(.125, level, 0);
		state.itemCount = itemCount;

		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack stack = ItemHandlerHelpers.getStackInSlot(inv, slot);
			if (stack.isEmpty())
				continue;
			state.ingredients.add(new StackedItem(
				DepotRenderer.ItemState.create(itemModelResolver, stack, basin.getLevel()), stack.getCount() / 8));
		}

		BlockState blockState = basin.getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return;
		Direction direction = blockState.getValue(BasinBlock.FACING);
		if (direction == Direction.DOWN)
			return;
		state.outputDirection = direction;
		state.outToBasin = basin.getLevel()
			.getBlockState(basin.getBlockPos()
				.relative(direction))
			.getBlock() instanceof BasinBlock;

		for (IntAttached<ItemStack> intAttached : basin.visualizedOutputItems) {
			float progress = 1 - (intAttached.getFirst() - partialTicks) / BasinBlockEntity.OUTPUT_ANIMATION_TIME;
			if (!state.outToBasin && progress > .35f)
				continue;
			state.outputs.add(new OutputItem(
				DepotRenderer.ItemState.create(itemModelResolver, intAttached.getValue(), basin.getLevel()), progress));
		}
	}

	@Override
	protected void submitSafe(BasinRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		for (FluidBox box : state.fluids)
			FluidRenderHelper.submitFluidBox(queue, box.fluid(), box.xMin(), box.yMin(), box.zMin(), box.xMax(),
				box.yMax(), box.zMax(), ms, state.lightCoords, false, false);

		ms.pushPose();
		ms.translate(.5, .2f, .5);
		TransformStack.of(ms)
			.rotateYDegrees(state.rotation);

		RandomSource r = RandomSource.create(state.seed);
		float anglePartition = 360f / state.itemCount;
		int itemCount = state.itemCount;

		for (StackedItem ingredient : state.ingredients) {
			ms.pushPose();

			if (state.fluidLevel > 0)
				ms.translate(0, (Mth.sin(state.bobPhase + anglePartition * itemCount) + 1.5f) * 1 / 32f, 0);

			Vec3 itemPosition = VecHelper.rotate(state.baseVector, anglePartition * itemCount, Axis.Y);
			ms.translate(itemPosition.x, itemPosition.y, itemPosition.z);
			TransformStack.of(ms)
				.rotateYDegrees(anglePartition * itemCount + 35)
				.rotateXDegrees(65);

			for (int i = 0; i <= ingredient.copies(); i++) {
				ms.pushPose();
				Vec3 vec = VecHelper.offsetRandomly(Vec3.ZERO, r, 1 / 16f);
				ms.translate(vec.x, vec.y, vec.z);
				submitItem(ms, queue, state.lightCoords, ingredient.item());
				ms.popPose();
			}
			ms.popPose();
			itemCount--;
		}
		ms.popPose();

		if (state.outputDirection == null)
			return;

		Direction direction = state.outputDirection;
		Vec3 directionVec = Vec3.atLowerCornerOf(direction.getUnitVec3i());
		Vec3 outVec = VecHelper.getCenterOf(BlockPos.ZERO)
			.add(directionVec.scale(.55)
				.subtract(0, 1 / 2f, 0));

		for (OutputItem output : state.outputs) {
			float progress = output.progress();
			ms.pushPose();
			TransformStack.of(ms)
				.translate(outVec)
				.translate(new Vec3(0, Math.max(-.55f, -(progress * progress * 2)), 0))
				.translate(directionVec.scale(progress * .5f))
				.rotateYDegrees(AngleHelper.horizontalAngle(direction))
				.rotateXDegrees(progress * 180);
			submitItem(ms, queue, state.lightCoords, output.item());
			ms.popPose();
		}
	}

	protected void submitItem(PoseStack ms, SubmitNodeCollector queue, int light, DepotRenderer.ItemState item) {
		item.item()
			.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
	}

	/**
	 * Fluids are laid out side by side across the basin, each taking a slice proportional to its
	 * share. The returned value is the surface height the ingredients float at.
	 */
	protected float extractFluids(BasinBlockEntity basin, BasinRenderState state, float partialTicks) {
		SmartFluidTankBehaviour inputFluids = basin.getBehaviour(SmartFluidTankBehaviour.INPUT);
		SmartFluidTankBehaviour outputFluids = basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT);
		SmartFluidTankBehaviour[] tanks = { inputFluids, outputFluids };
		float totalUnits = basin.getTotalFluidUnits(partialTicks);
		if (totalUnits < 1)
			return 0;

		float fluidLevel = Mth.clamp(totalUnits / 2000, 0, 1);
		fluidLevel = 1 - ((1 - fluidLevel) * (1 - fluidLevel));

		float xMin = 2 / 16f;
		float xMax = 2 / 16f;
		final float yMin = 2 / 16f;
		final float yMax = yMin + 12 / 16f * fluidLevel;
		final float zMin = 2 / 16f;
		final float zMax = 14 / 16f;

		for (SmartFluidTankBehaviour behaviour : tanks) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				FluidStack renderedFluid = tankSegment.getRenderedFluid();
				if (renderedFluid.isEmpty())
					continue;
				float units = tankSegment.getTotalUnits(partialTicks);
				if (units < 1)
					continue;

				float partial = Mth.clamp(units / totalUnits, 0, 1);
				xMax += partial * 12 / 16f;
				state.fluids.add(new FluidBox(renderedFluid.copy(), xMin, yMin, zMin, xMax, yMax, zMax));
				xMin = xMax;
			}
		}

		return yMax;
	}

	@Override
	public int getViewDistance() {
		return 16;
	}

}
