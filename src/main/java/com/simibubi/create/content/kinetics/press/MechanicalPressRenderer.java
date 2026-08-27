package com.simibubi.create.content.kinetics.press;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MechanicalPressRenderer
	extends KineticBlockEntityRenderer<MechanicalPressBlockEntity, MechanicalPressRenderer.PressRenderState> {

	public static class PressRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState head;
	}

	public MechanicalPressRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public PressRenderState createRenderState() {
		return new PressRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected void extractSafe(MechanicalPressBlockEntity be, PressRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		BlockState blockState = be.getBlockState();
		PressingBehaviour pressingBehaviour = be.getPressingBehaviour();
		float renderedHeadOffset =
			pressingBehaviour.getRenderedHeadOffset(partialTicks) * pressingBehaviour.mode.headOffset;

		SuperByteBuffer headRender = CachedBuffers.partialFacing(AllPartialModels.MECHANICAL_PRESS_HEAD, blockState,
			blockState.getValue(HORIZONTAL_FACING));
		TransformStack.of(headRender.getTransforms())
			.translate(0, -renderedHeadOffset, 0);
		state.head = headRender.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(PressRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.head != null)
			state.head.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected BlockState getRenderedBlockState(MechanicalPressBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
