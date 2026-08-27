package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import com.simibubi.create.content.logistics.depot.DepotRenderer.ItemState;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import java.util.Random;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.ShadowRenderHelper;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.level.wrapper.WrappedLevel;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BeltRenderer extends SafeBlockEntityRenderer<BeltBlockEntity, BeltRenderer.BeltRenderState> {

	public static class BeltRenderState extends SafeRenderState {
		/** Belt casing and pulley, each already carrying its own transform. */
		public final List<SuperByteBufferRenderState> belt = new ArrayList<>();
		public final List<BeltItemState> items = new ArrayList<>();
		public Vec3 beltStartOffset = Vec3.ZERO;
	}

	/**
	 * One item riding the belt. Everything the submit pass positions it by is resolved during
	 * extraction, since it all comes off the belt inventory or the level.
	 */
	public record BeltItemState(ItemState item, int angle, Vec3 offset, float sideOffset, boolean alongX,
		boolean onSlope, boolean slopeShadowOnly, float slopeAngle, boolean slopeAlongX, int light,
		Vec3 itemPosition) {
	}

	protected final ItemModelResolver itemModelResolver;

	public BeltRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	/**
	 * 26.2 dropped the block entity argument. Only the controller extracts items, and the casing is
	 * cheap, so every belt segment opts in.
	 */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public BeltRenderState createRenderState() {
		return new BeltRenderState();
	}

	@Override
	protected void extractSafe(BeltBlockEntity be, BeltRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.belt.clear();
		state.items.clear();

		if (!VisualizationManager.supportsVisualization(be.getLevel()))
			extractBelt(be, state);

		extractItems(be, state, partialTicks);
	}

	private static void extractBelt(BeltBlockEntity be, BeltRenderState state) {
		BlockState blockState = be.getBlockState();
		if (!AllBlocks.BELT.has(blockState))
			return;

		BeltSlope beltSlope = blockState.getValue(BeltBlock.SLOPE);
		BeltPart part = blockState.getValue(BeltBlock.PART);
		Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
		AxisDirection axisDirection = facing.getAxisDirection();

		boolean downward = beltSlope == BeltSlope.DOWNWARD;
		boolean upward = beltSlope == BeltSlope.UPWARD;
		boolean diagonal = downward || upward;
		boolean start = part == BeltPart.START;
		boolean end = part == BeltPart.END;
		boolean sideways = beltSlope == BeltSlope.SIDEWAYS;
		boolean alongX = facing.getAxis() == Direction.Axis.X;

		PoseStack localTransforms = new PoseStack();
		var msr = TransformStack.of(localTransforms);
		float renderTick = AnimationTickHolder.getRenderTime(be.getLevel());
		int light = state.lightCoords;

		msr.center()
				.rotateYDegrees(AngleHelper.horizontalAngle(facing) + (upward ? 180 : 0) + (sideways ? 270 : 0))
				.rotateZDegrees(sideways ? 90 : 0)
				.rotateXDegrees(!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0)
				.uncenter();

		if (downward || beltSlope == BeltSlope.VERTICAL && axisDirection == AxisDirection.POSITIVE) {
			boolean b = start;
			start = end;
			end = b;
		}

		DyeColor color = be.color.orElse(null);

		for (boolean bottom : Iterate.trueAndFalse) {

			PartialModel beltPartial = getBeltPartial(diagonal, start, end, bottom);

			SuperByteBuffer beltBuffer = CachedBufferer.partial(beltPartial, blockState)
				.light(light);

			SpriteShiftEntry spriteShift = getSpriteShiftEntry(color, diagonal, bottom);

			// UV shift
			float speed = be.getSpeed();
			if (speed != 0 || be.color.isPresent()) {
				float time = renderTick * axisDirection.getStep();
				if (diagonal && (downward ^ alongX) || !sideways && !diagonal && alongX
					|| sideways && axisDirection == AxisDirection.NEGATIVE)
					speed = -speed;

				float scrollMult = diagonal ? 3f / 8f : 0.5f;

				float spriteSize = spriteShift.getTarget()
					.getV1()
					- spriteShift.getTarget()
						.getV0();

				double scroll = speed * time / (31.5 * 16) + (bottom ? 0.5 : 0.0);
				scroll = scroll - Math.floor(scroll);
				scroll = scroll * spriteSize * scrollMult;

				beltBuffer.shiftUVScrolling(spriteShift, (float) scroll);
			}

			state.belt.add(beltBuffer.transform(localTransforms)
				.extractRenderState());

			// Diagonal belt do not have a separate bottom model
			if (diagonal)
				break;
		}

		if (be.hasPulley()) {
			Direction dir = sideways ? Direction.UP
				: blockState.getValue(BeltBlock.HORIZONTAL_FACING)
					.getClockWise();

			Supplier<PoseStack> matrixStackSupplier = () -> {
				PoseStack stack = new PoseStack();
				var stacker = TransformStack.of(stack);
				stacker.center();
				if (dir.getAxis() == Direction.Axis.X) stacker.rotateYDegrees(90);
				if (dir.getAxis() == Direction.Axis.Y) stacker.rotateXDegrees(90);
				stacker.rotateXDegrees(90);
				stacker.uncenter();
				return stack;
			};

			SuperByteBuffer superBuffer = CachedBufferer.partialDirectional(AllPartialModels.BELT_PULLEY,
				blockState, dir, matrixStackSupplier);
			state.belt.add(KineticBlockEntityRenderer.standardKineticRotationTransform(superBuffer, be, light)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(BeltRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState buffer : state.belt)
			buffer.submit(ms, RenderTypes.solidMovingBlock(), queue);
		submitItems(state, ms, queue);
	}

	public static SpriteShiftEntry getSpriteShiftEntry(DyeColor color, boolean diagonal, boolean bottom) {
		if (color != null) {
			return (diagonal ? AllSpriteShifts.DYED_DIAGONAL_BELTS
				: bottom ? AllSpriteShifts.DYED_OFFSET_BELTS : AllSpriteShifts.DYED_BELTS).get(color);
		} else
			return diagonal ? AllSpriteShifts.BELT_DIAGONAL
				: bottom ? AllSpriteShifts.BELT_OFFSET : AllSpriteShifts.BELT;
	}

	public static PartialModel getBeltPartial(boolean diagonal, boolean start, boolean end, boolean bottom) {
		if (diagonal) {
			if (start)
				return AllPartialModels.BELT_DIAGONAL_START;
			if (end)
				return AllPartialModels.BELT_DIAGONAL_END;
			return AllPartialModels.BELT_DIAGONAL_MIDDLE;
		} else if (bottom) {
			if (start)
				return AllPartialModels.BELT_START_BOTTOM;
			if (end)
				return AllPartialModels.BELT_END_BOTTOM;
			return AllPartialModels.BELT_MIDDLE_BOTTOM;
		} else {
			if (start)
				return AllPartialModels.BELT_START;
			if (end)
				return AllPartialModels.BELT_END;
			return AllPartialModels.BELT_MIDDLE;
		}
	}

	private void extractItems(BeltBlockEntity be, BeltRenderState state, float partialTicks) {
		if (!be.isController())
			return;
		if (be.beltLength == 0)
			return;

		Direction beltFacing = be.getBeltFacing();
		Vec3i directionVec = beltFacing.getUnitVec3i();
		state.beltStartOffset = Vec3.atLowerCornerOf(directionVec)
			.scale(-.5)
			.add(.5, 15 / 16f, .5);
		BeltSlope slope = be.getBlockState()
			.getValue(BeltBlock.SLOPE);
		int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;
		boolean slopeAlongX = beltFacing.getAxis() == Direction.Axis.X;
		boolean onContraption = be.getLevel() instanceof WrappedLevel;

		BeltInventory inventory = be.getInventory();
		for (TransportedItemStack transported : inventory.getTransportedItems())
			extractItem(be, state, partialTicks, beltFacing, directionVec, slope, verticality, slopeAlongX,
				onContraption, transported);
		if (inventory.getLazyClientItem() != null)
			extractItem(be, state, partialTicks, beltFacing, directionVec, slope, verticality, slopeAlongX,
				onContraption, inventory.getLazyClientItem());
	}

	private void extractItem(BeltBlockEntity be, BeltRenderState state, float partialTicks, Direction beltFacing,
		Vec3i directionVec, BeltSlope slope, int verticality, boolean slopeAlongX, boolean onContraption,
		TransportedItemStack transported) {
		MutableBlockPos mutablePos = new MutableBlockPos();

		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);
		float verticalMovement;

		if (be.getSpeed() == 0) {
			offset = transported.beltPosition;
			sideOffset = transported.sideOffset;
		}

		if (offset < .5)
			verticalMovement = 0;
		else
			verticalMovement = verticality * (Math.min(offset, be.beltLength - .5f) - .5f);
		Vec3 offsetVec = Vec3.atLowerCornerOf(directionVec)
			.scale(offset);
		if (verticalMovement != 0)
			offsetVec = offsetVec.add(0, verticalMovement, 0);
		boolean onSlope = slope != BeltSlope.HORIZONTAL && Mth.clamp(offset, .5f, be.beltLength - .5f) == offset;
		boolean tiltForward = (slope == BeltSlope.DOWNWARD
			^ beltFacing.getAxisDirection() == AxisDirection.POSITIVE) == (beltFacing.getAxis() == Direction.Axis.Z);
		float slopeAngle = onSlope ? tiltForward ? -45 : 45 : 0;

		Vec3 itemPos = state.beltStartOffset.add(
				be.getBlockPos().getX(),
				be.getBlockPos().getY(),
				be.getBlockPos().getZ())
			.add(offsetVec);

		if (this.shouldCullItem(itemPos, be.getLevel()))
			return;

		boolean alongX = beltFacing.getClockWise()
			.getAxis() == Direction.Axis.X;
		if (!alongX)
			sideOffset *= -1;

		int stackLight;
		if (onContraption) {
			stackLight = state.lightCoords;
		} else {
			int segment = (int) Math.floor(offset);
			mutablePos.set(be.getBlockPos()).move(directionVec.getX() * segment, verticality * segment, directionVec.getZ() * segment);
			stackLight = LightCoordsUtil.getLightCoords(be.getLevel(), mutablePos);
		}

		ItemState item = ItemState.create(itemModelResolver, transported.stack, be.getLevel());
		// Stacks only get their extra copies drawn up close, or in ponder where the camera is fixed.
		if (!(be.getLevel() instanceof PonderLevel) && Minecraft.getInstance().player.getEyePosition(1.0F)
			.distanceTo(itemPos) >= 16)
			item = new ItemState(item.item(), item.blockItem(), item.upright(), item.box(), 0);
		boolean renderUpright = item.upright();

		// The upright billboard needs the item's position on the belt, which is the belt's own
		// interpolated position rather than the block centre.
		Vec3 uprightAnchor = renderUpright ? BeltHelper.getVectorForOffset(be, offset) : Vec3.ZERO;

		state.items.add(new BeltItemState(item, transported.angle, offsetVec, sideOffset, alongX, onSlope,
			renderUpright && onSlope, slopeAngle, slopeAlongX, stackLight, uprightAnchor));
	}

	private static void submitItems(BeltRenderState state, PoseStack ms, SubmitNodeCollector queue) {
		if (state.items.isEmpty())
			return;

		ms.pushPose();
		ms.translate(state.beltStartOffset.x, state.beltStartOffset.y, state.beltStartOffset.z);

		for (BeltItemState transported : state.items)
			submitItem(state, ms, queue, transported);

		ms.popPose();
	}

	private static void submitItem(BeltRenderState state, PoseStack ms, SubmitNodeCollector queue,
		BeltItemState transported) {
		ItemState item = transported.item();
		boolean blockItem = item.blockItem();
		boolean renderUpright = item.upright();

		ms.pushPose();
		TransformStack.of(ms).nudge(transported.angle());
		Vec3 offsetVec = transported.offset();
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		ms.translate(transported.alongX() ? transported.sideOffset() : 0, 0,
			transported.alongX() ? 0 : transported.sideOffset());

		boolean slopeShadowOnly = transported.slopeShadowOnly();
		float slopeOffset = 1 / 8f;
		if (slopeShadowOnly)
			ms.pushPose();
		if (!renderUpright || slopeShadowOnly)
			ms.mulPose((transported.slopeAlongX() ? Axis.ZP : Axis.XP).rotationDegrees(transported.slopeAngle()));
		if (transported.onSlope())
			ms.translate(0, slopeOffset, 0);
		ms.pushPose();
		ms.translate(0, -1 / 8f + 0.005f, 0);
		ShadowRenderHelper.submitShadow(ms, queue, .75f, .2f);
		ms.popPose();
		if (slopeShadowOnly) {
			ms.popPose();
			ms.translate(0, slopeOffset, 0);
		}

		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera().position();
			Vec3 diff = transported.itemPosition().subtract(cameraPosition);
			float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
			ms.mulPose(Axis.YP.rotation(yRot));
			ms.translate(0, 3 / 32d, 1 / 16f);
		}

		Random r = new Random(transported.angle());
		int count = item.count();

		for (int i = 0; i <= count; i++) {
			ms.pushPose();

			boolean box = item.box();
			ms.mulPose(Axis.YP.rotationDegrees(transported.angle()));
			if (!blockItem && !renderUpright) {
				ms.translate(0, -.09375, 0);
				ms.mulPose(Axis.XP.rotationDegrees(90));
			}

			if (blockItem && !box)
				ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);

			if (box) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(.5f, .5f, .5f);
			}

			item.item()
				.submit(ms, queue, transported.light(), OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();

			if (!renderUpright) {
				if (!blockItem)
					ms.mulPose(Axis.YP.rotationDegrees(10));
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);

		}

		ms.popPose();
	}
}
