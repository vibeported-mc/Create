package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.foundation.render.RenderLevels;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PortableStorageInterfaceRenderer
	extends SafeBlockEntityRenderer<PortableStorageInterfaceBlockEntity, PortableStorageInterfaceRenderer.PsiRenderState> {

	public static class PsiRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>(2);
	}

	public PortableStorageInterfaceRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public PsiRenderState createRenderState() {
		return new PsiRenderState();
	}

	@Override
	protected void extractSafe(PortableStorageInterfaceBlockEntity be, PsiRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.parts.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		float progress = be.getExtensionDistance(partialTicks);
		transform(blockState, be.isConnected(), progress, null,
			sbb -> state.parts.add(sbb.light(state.lightCoords)
				.extractRenderState()));
	}

	@Override
	protected void submitSafe(PsiRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		BlockState blockState = context.state;
		float renderPartialTicks = AnimationTickHolder.getPartialTicks();

		LerpedFloat animation = PortableStorageInterfaceMovement.getAnimation(context);
		float progress = animation.getValue(renderPartialTicks);
		boolean lit = animation.settled();
		transform(blockState, lit, progress, matrices.getModel(),
			sbb -> {
				sbb.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
					.useLevelLight(RenderLevels.lightSource(context.world, renderWorld), matrices.getWorld());
				out.add(ActorGeometry.of(matrices.getViewProjection(), sbb, RenderTypes.solidMovingBlock()));
			});
	}

	/**
	 * Builds the two moving pieces and hands each to the callback, which either extracts it for a
	 * block entity render state or submits it directly from a contraption.
	 */
	private static void transform(BlockState blockState, boolean lit, float progress, PoseStack local,
		Consumer<SuperByteBuffer> drawCallback) {
		SuperByteBuffer middle = CachedBufferer.partial(getMiddleForState(blockState, lit), blockState);
		SuperByteBuffer top = CachedBufferer.partial(getTopForState(blockState), blockState);

		if (local != null) {
			middle.transform(local);
			top.transform(local);
		}
		Direction facing = blockState.getValue(PortableStorageInterfaceBlock.FACING);
		rotateToFacing(middle, facing);
		rotateToFacing(top, facing);
		TransformStack.of(middle.getTransforms())
			.translate(0, progress * 0.5f + 0.375f, 0);
		TransformStack.of(top.getTransforms())
			.translate(0, progress, 0);

		drawCallback.accept(middle);
		drawCallback.accept(top);
	}

	private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
		TransformStack.of(buffer.getTransforms())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
			.uncenter();
	}

	static PortableStorageInterfaceBlockEntity getTargetPSI(MovementContext context) {
		String _workingPos_ = PortableStorageInterfaceMovement._workingPos_;
		if (!context.data.contains(_workingPos_))
			return null;

		BlockPos pos = NBTHelper.readBlockPos(context.data, _workingPos_);
		BlockEntity blockEntity = context.world.getBlockEntity(pos);
		if (!(blockEntity instanceof PortableStorageInterfaceBlockEntity psi))
			return null;

		if (!psi.isTransferring())
			return null;
		return psi;
	}

	static PartialModel getMiddleForState(BlockState state, boolean lit) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return lit ? AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE_POWERED
				: AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE;
		return lit ? AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE_POWERED
			: AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE;
	}

	static PartialModel getTopForState(BlockState state) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return AllPartialModels.PORTABLE_FLUID_INTERFACE_TOP;
		return AllPartialModels.PORTABLE_STORAGE_INTERFACE_TOP;
	}

}
