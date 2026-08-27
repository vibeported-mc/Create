package com.simibubi.create.content.logistics.depot;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.Rotate;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.data.IntAttached;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class EjectorRenderer extends ShaftRenderer<EjectorBlockEntity, EjectorRenderer.EjectorRenderState> {

	public static class EjectorRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState lid;
		public final List<LaunchedItem> launched = new ArrayList<>();
		public final DepotRenderer.DepotRenderState depot = new DepotRenderer.DepotRenderState();
		public boolean hasDepotItems;
		public float lidAngle;
		public float horizontalAngle;
		public Direction facing = Direction.NORTH;
	}

	public record LaunchedItem(DepotRenderer.ItemState item, float time, Vec3 offset, boolean isPackage) {
	}

	protected final ItemModelResolver itemModelResolver;

	static final Vec3 pivot = VecHelper.voxelSpace(0, 11.25, 0.75);

	public EjectorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.itemModelResolver();
	}

	/**
	 * 26.2 dropped the block entity argument; ejectors always wanted this anyway.
	 */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public EjectorRenderState createRenderState() {
		return new EjectorRenderState();
	}

	@Override
	protected void extractSafe(EjectorBlockEntity be, EjectorRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.lid = null;
		state.launched.clear();
		state.hasDepotItems = false;

		float lidProgress = be.getLidProgress(partialTicks);
		state.lidAngle = lidProgress * 70;
		state.facing = be.getFacing();
		state.horizontalAngle = AngleHelper.horizontalAngle(be.getBlockState()
			.getValue(EjectorBlock.HORIZONTAL_FACING));

		if (!VisualizationManager.supportsVisualization(be.getLevel())) {
			SuperByteBuffer model = CachedBuffers.partial(AllPartialModels.EJECTOR_TOP, be.getBlockState());
			applyLidAngle(be, state.lidAngle, TransformStack.of(model.getTransforms()));
			state.lid = model.light(state.lightCoords)
				.extractRenderState();
		}

		float maxTime = (float) (be.earlyTarget != null ? be.earlyTargetTime : be.launcher.getTotalFlyingTicks());
		for (IntAttached<ItemStack> intAttached : be.launchedItems) {
			float time = intAttached.getFirst() + partialTicks;
			if (time > maxTime)
				continue;
			ItemStack stack = intAttached.getValue();
			state.launched.add(new LaunchedItem(
				DepotRenderer.ItemState.create(itemModelResolver, stack, be.getLevel()), time,
				be.getLaunchedItemLocation(time)
					.subtract(Vec3.atLowerCornerOf(be.getBlockPos())),
				PackageItem.isPackage(stack)));
		}

		DepotBehaviour behaviour = be.getBehaviour(DepotBehaviour.TYPE);
		if (behaviour == null || behaviour.isEmpty())
			return;
		state.hasDepotItems = true;
		DepotRenderer.extractItemsOf(be, state.depot, partialTicks, behaviour, itemModelResolver);
	}

	@Override
	protected void submitSafe(EjectorRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);

		if (state.lid != null)
			state.lid.submit(ms, RenderTypes.solidMovingBlock(), queue);

		var msr = TransformStack.of(ms);
		Vec3 itemRotOffset = VecHelper.voxelSpace(0, 2, -1);

		for (LaunchedItem launched : state.launched) {
			ms.pushPose();
			msr.translate(launched.offset());
			msr.translate(itemRotOffset);

			if (launched.isPackage()) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
				msr.rotateYDegrees(launched.time() * 20);
			} else {
				ms.scale(.5f, .5f, .5f);
				msr.rotateYDegrees(AngleHelper.horizontalAngle(state.facing));
				msr.rotateXDegrees(launched.time() * 40);
			}
			msr.translateBack(itemRotOffset);
			launched.item()
				.item()
				.submit(ms, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		if (!state.hasDepotItems)
			return;

		ms.pushPose();
		applyLidAngle(state.horizontalAngle, state.lidAngle, msr);
		msr.center()
			.rotateYDegrees(-180 - state.horizontalAngle)
			.uncenter();
		DepotRenderer.submitItemsOf(state.depot, ms, queue, state.lightCoords);
		ms.popPose();
	}

	/**
	 * Submit-time variant: the block state is no longer reachable, so the facing angle is passed in.
	 */
	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(float horizontalAngle, float angle, T tr) {
		tr.center()
			.rotateYDegrees(180 + horizontalAngle)
			.uncenter()
			.translate(pivot)
			.rotateXDegrees(-angle)
			.translateBack(pivot);
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, float angle, T tr) {
		applyLidAngle(be, pivot, angle, tr);
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, Vec3 rotationOffset, float angle, T tr) {
		tr.center()
			.rotateYDegrees(180 + AngleHelper.horizontalAngle(be.getBlockState()
				.getValue(EjectorBlock.HORIZONTAL_FACING)))
			.uncenter()
			.translate(rotationOffset)
			.rotateXDegrees(-angle)
			.translateBack(rotationOffset);
	}

}
