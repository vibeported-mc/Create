package com.simibubi.create.content.contraptions.actors.harvester;

import com.simibubi.create.foundation.render.RenderLevels;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import java.util.List;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HarvesterRenderer
	extends SafeBlockEntityRenderer<HarvesterBlockEntity, HarvesterRenderer.HarvesterRenderState> {

	private static final Vec3 PIVOT = new Vec3(0, 6, 9);

	public static class HarvesterRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState blade;
	}

	public HarvesterRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public HarvesterRenderState createRenderState() {
		return new HarvesterRenderState();
	}

	@Override
	protected void extractSafe(HarvesterBlockEntity be, HarvesterRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		BlockState blockState = be.getBlockState();
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.HARVESTER_BLADE, blockState);
		transform(be.getLevel(), blockState.getValue(HarvesterBlock.FACING), superBuffer, be.getAnimatedSpeed(), PIVOT);
		state.blade = superBuffer.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(HarvesterRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.blade != null)
			state.blade.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	public static void extractInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		BlockState blockState = context.state;
		Direction facing = blockState.getValue(HORIZONTAL_FACING);
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.HARVESTER_BLADE, blockState);
		float speed = (float) (!VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: 0);
		if (context.contraption.stalled)
			speed = 0;

		superBuffer.transform(matrices.getModel());
		transform(context.world, facing, superBuffer, speed, PIVOT);

		superBuffer.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
			.useLevelLight(RenderLevels.lightSource(context.world, renderWorld), matrices.getWorld());
		out.add(ActorGeometry.of(matrices.getViewProjection(), superBuffer, RenderTypes.cutoutMovingBlock()));
	}

	public static void transform(Level world, Direction facing, SuperByteBuffer superBuffer, float speed, Vec3 pivot) {
		float originOffset = 1 / 16f;
		Vec3 rotOffset = new Vec3(0, pivot.y * originOffset, pivot.z * originOffset);
		float time = AnimationTickHolder.getRenderTime(world) / 20;
		float angle = (time * speed) % 360;

		TransformStack.of(superBuffer.getTransforms())
			.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing)), Direction.UP)
			.translate(rotOffset.x, rotOffset.y, rotOffset.z)
			.rotate(AngleHelper.rad(angle), Direction.WEST)
			.translate(-rotOffset.x, -rotOffset.y, -rotOffset.z);
	}
}
