package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ToolboxRenderer extends SmartBlockEntityRenderer<ToolboxBlockEntity, ToolboxRenderer.ToolboxRenderState> {

	public static class ToolboxRenderState extends SmartRenderState {
		public @Nullable SuperByteBufferRenderState lid;
		public final List<SuperByteBufferRenderState> drawers = new ArrayList<>(2);
	}

	public ToolboxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ToolboxRenderState createRenderState() {
		return new ToolboxRenderState();
	}

	@Override
	protected void extractSafe(ToolboxBlockEntity be, ToolboxRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.drawers.clear();

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(ToolboxBlock.FACING)
			.getOpposite();

		float lidAngle = be.lid.getValue(partialTicks);
		float drawerOffset = be.drawers.getValue(partialTicks);

		SuperByteBuffer lid = CachedBufferer.partial(AllPartialModels.TOOLBOX_LIDS.get(be.getColor()), blockState);
		TransformStack.of(lid.getTransforms())
			.center()
			.rotateYDegrees(-facing.toYRot())
			.uncenter()
			.translate(0, 6 / 16f, 12 / 16f)
			.rotateXDegrees(135 * lidAngle)
			.translate(0, -6 / 16f, -12 / 16f);
		state.lid = lid.light(state.lightCoords)
			.extractRenderState();

		// The two drawers sit at different offsets, so each needs its own buffer.
		for (int offset : Iterate.zeroAndOne) {
			SuperByteBuffer drawer = CachedBufferer.partial(AllPartialModels.TOOLBOX_DRAWER, blockState);
			TransformStack.of(drawer.getTransforms())
				.center()
				.rotateYDegrees(-facing.toYRot())
				.uncenter()
				.translate(0, offset * 1 / 8f, -drawerOffset * .175f * (2 - offset));
			state.drawers.add(drawer.light(state.lightCoords)
				.extractRenderState());
		}
	}

	@Override
	protected void submitSafe(ToolboxRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.lid != null)
			state.lid.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		for (SuperByteBufferRenderState drawer : state.drawers)
			drawer.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

}
