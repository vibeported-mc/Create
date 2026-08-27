package com.simibubi.create.content.contraptions.pulley;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.infrastructure.config.AllConfigs;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractPulleyRenderer<T extends KineticBlockEntity, S extends AbstractPulleyRenderer.PulleyRenderState>
	extends KineticBlockEntityRenderer<T, S> {

	public static class PulleyRenderState extends KineticRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
	}

	private PartialModel halfRope;
	private PartialModel halfMagnet;

	public AbstractPulleyRenderer(BlockEntityRendererProvider.Context context, PartialModel halfRope,
		PartialModel halfMagnet) {
		super(context);
		this.halfRope = halfRope;
		this.halfMagnet = halfMagnet;
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new PulleyRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected void extractSafe(T be, S state, float partialTicks, Vec3 cameraPosition) {
		state.parts.clear();

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			state.skip = true;
			return;
		}

		super.extractSafe(be, state, partialTicks, cameraPosition);

		float offset = getOffset(be, partialTicks);
		boolean running = isRunning(be);

		state.parts.add(scrollCoil(getRotatedCoil(be), getCoilShift(), offset, 1).light(state.lightCoords)
			.extractRenderState());

		Level world = be.getLevel();
		BlockState blockState = be.getBlockState();
		BlockPos pos = be.getBlockPos();

		if (running || offset == 0) {
			SuperByteBuffer magnet = offset > .25f ? renderMagnet(be)
				: CachedBufferer.partial(this.halfMagnet, blockState);
			state.parts.add(extractAt(world, magnet, offset, pos));
		}

		float f = offset % 1;
		if (offset > .75f && (f < .25f || f > .75f))
			state.parts.add(extractAt(world, CachedBufferer.partial(this.halfRope, blockState),
				f > .75f ? f - 1 : f, pos));

		if (!running)
			return;

		// Each rope segment sits at a different height, so it needs its own buffer: one extracted
		// state cannot carry several transforms.
		for (int i = 0; i < offset - 1.25f; i++)
			state.parts.add(extractAt(world, renderRope(be), offset - i - 1, pos));
	}

	@Override
	protected void submitSafe(S state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public static SuperByteBufferRenderState extractAt(LevelAccessor world, SuperByteBuffer partial, float offset,
		BlockPos pulleyPos) {
		BlockPos actualPos = pulleyPos.below((int) offset);
		int light = LightCoordsUtil.getLightCoords(world, world.getBlockState(actualPos), actualPos);
		TransformStack.of(partial.getTransforms())
			.translate(0, -offset, 0);
		return partial.light(light)
			.extractRenderState();
	}

	protected abstract Axis getShaftAxis(T be);

	protected abstract PartialModel getCoil();

	protected abstract SpriteShiftEntry getCoilShift();

	protected abstract SuperByteBuffer renderRope(T be);

	protected abstract SuperByteBuffer renderMagnet(T be);

	protected abstract float getOffset(T be, float partialTicks);

	protected abstract boolean isRunning(T be);

	@Override
	protected BlockState getRenderedBlockState(T be) {
		return shaft(getShaftAxis(be));
	}

	protected SuperByteBuffer getRotatedCoil(T be) {
		BlockState blockState = be.getBlockState();
		return CachedBufferer.partialFacing(getCoil(), blockState,
			Direction.get(AxisDirection.POSITIVE, getShaftAxis(be)));
	}

	public static SuperByteBuffer scrollCoil(SuperByteBuffer sbb, SpriteShiftEntry coilShift, float offset,
		float speedModifier) {
		if (offset == 0)
			return sbb;
		float spriteSize = coilShift.getTarget()
			.getV1()
			- coilShift.getTarget()
				.getV0();
		offset *= speedModifier / 2;
		double coilScroll = -(offset + 3 / 16f) - Math.floor((offset + 3 / 16f) * -2) / 2;
		return sbb.shiftUVScrolling(coilShift, (float) coilScroll * spriteSize);
	}

	@Override
	public int getViewDistance() {
		return AllConfigs.server().kinetics.maxRopeLength.get();
	}

}
