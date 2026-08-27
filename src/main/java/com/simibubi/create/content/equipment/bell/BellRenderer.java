package com.simibubi.create.content.equipment.bell;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.Vec3;

public class BellRenderer<BE extends AbstractBellBlockEntity>
	extends SafeBlockEntityRenderer<BE, BellRenderer.BellRenderState> {

	public static class BellRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState bell;
	}

	public BellRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BellRenderState createRenderState() {
		return new BellRenderState();
	}

	@Override
	protected void extractSafe(BE be, BellRenderState state, float partialTicks, Vec3 cameraPosition) {
		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(BellBlock.FACING);
		BellAttachType attachment = blockState.getValue(BellBlock.ATTACHMENT);

		SuperByteBuffer bell = CachedBuffers.partial(be.getBellModel(), blockState);
		var msr = TransformStack.of(bell.getTransforms());

		if (be.isRinging)
			msr.rotateCentered(getSwingAngle(be.ringingTicks + partialTicks),
				be.ringDirection.getCounterClockWise());

		float rY = AngleHelper.horizontalAngle(facing);
		if (attachment == BellAttachType.SINGLE_WALL || attachment == BellAttachType.DOUBLE_WALL)
			rY += 90;
		msr.rotateCentered(AngleHelper.rad(rY), Direction.UP);

		state.bell = bell.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(BellRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.bell != null)
			state.bell.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	public static float getSwingAngle(float time) {
		float t = time / 1.5f;
		return 1.2f * Mth.sin(t / (float) Math.PI) / (2.5f + t / 3.0f);
	}

}
