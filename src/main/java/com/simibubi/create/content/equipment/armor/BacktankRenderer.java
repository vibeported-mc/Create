package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BacktankRenderer
	extends KineticBlockEntityRenderer<BacktankBlockEntity, BacktankRenderer.BacktankRenderState> {

	public static class BacktankRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState cogs;
	}

	public BacktankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BacktankRenderState createRenderState() {
		return new BacktankRenderState();
	}

	@Override
	protected void extractSafe(BacktankBlockEntity be, BacktankRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);

		BlockState blockState = be.getBlockState();
		SuperByteBuffer cogs = CachedBufferer.partial(getCogsModel(blockState), blockState);
		TransformStack.of(cogs.getTransforms())
			.center()
			.rotateYDegrees(180 + AngleHelper.horizontalAngle(blockState.getValue(BacktankBlock.HORIZONTAL_FACING)))
			.uncenter()
			.translate(0, 6.5f / 16, 11f / 16)
			.rotate(AngleHelper.rad(be.getSpeed() / 4f * AnimationTickHolder.getRenderTime(be.getLevel()) % 360),
				Direction.EAST)
			.translate(0, -6.5f / 16, -11f / 16);
		state.cogs = cogs.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(BacktankRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.cogs != null)
			state.cogs.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(BacktankBlockEntity be, BlockState state) {
		return CachedBufferer.partial(getShaftModel(state), state);
	}

	public static PartialModel getCogsModel(BlockState state) {
		if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
			return AllPartialModels.NETHERITE_BACKTANK_COGS;
		}
		return AllPartialModels.COPPER_BACKTANK_COGS;
	}

	public static PartialModel getShaftModel(BlockState state) {
		if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
			return AllPartialModels.NETHERITE_BACKTANK_SHAFT;
		}
		return AllPartialModels.COPPER_BACKTANK_SHAFT;
	}
}
