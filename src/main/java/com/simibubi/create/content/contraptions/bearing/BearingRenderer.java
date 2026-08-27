package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class BearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity>
	extends KineticBlockEntityRenderer<T, BearingRenderer.BearingRenderState> {

	public static class BearingRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState top;
	}

	public BearingRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BearingRenderState createRenderState() {
		return new BearingRenderState();
	}

	@Override
	protected void extractSafe(T be, BearingRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.top = null;

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		super.extractSafe(be, state, partialTicks, cameraPosition);

		final Direction facing = be.getBlockState()
			.getValue(BlockStateProperties.FACING);
		PartialModel top = be.isWoodenTop() ? AllPartialModels.BEARING_TOP_WOODEN : AllPartialModels.BEARING_TOP;
		SuperByteBuffer superBuffer = CachedBufferer.partial(top, be.getBlockState());

		float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1);
		kineticRotationTransform(superBuffer, be, facing.getAxis(), (float) (interpolatedAngle / 180 * Math.PI),
			state.lightCoords);

		var msr = TransformStack.of(superBuffer.getTransforms());
		if (facing.getAxis()
			.isHorizontal())
			msr.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
		msr.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);

		state.top = superBuffer.extractRenderState();
	}

	@Override
	protected void submitSafe(BearingRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.top != null)
			state.top.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
		return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, state, state
			.getValue(BearingBlock.FACING)
			.getOpposite());
	}

}
