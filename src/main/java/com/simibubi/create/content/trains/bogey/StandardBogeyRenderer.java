package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.foundation.render.CachedBufferer;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class StandardBogeyRenderer implements BogeyRenderer {

	private static final RenderType RENDER_TYPE = RenderTypes.cutoutMovingBlock();

	/**
	 * Each piece carries its own transform, so a buffer that used to be reused across a loop is
	 * fetched fresh per iteration - one extracted state holds exactly one transform.
	 */
	protected static void add(List<Part> out, SuperByteBuffer buffer, int light) {
		out.add(new Part(buffer.light(light)
			.extractRenderState(), RENDER_TYPE));
	}

	@Override
	public void extract(CompoundTag bogeyData, float wheelAngle, float partialTick, int light, boolean inContraption,
		List<Part> out) {
		for (int i : Iterate.zeroAndOne) {
			SuperByteBuffer shaft = CachedBuffers.block(AllBlocks.SHAFT.getDefaultState()
				.setValue(ShaftBlock.AXIS, Direction.Axis.Z));
			TransformStack.of(shaft.getTransforms())
				.translate(-.5f, .25f, i * -1)
				.center()
				.rotateZDegrees(wheelAngle)
				.uncenter();
			add(out, shaft, light);
		}
	}

	public static class Small extends StandardBogeyRenderer {
		@Override
		public void extract(CompoundTag bogeyData, float wheelAngle, float partialTick, int light,
			boolean inContraption, List<Part> out) {
			super.extract(bogeyData, wheelAngle, partialTick, light, inContraption, out);

			SuperByteBuffer frame = CachedBufferer.partial(AllPartialModels.BOGEY_FRAME, Blocks.AIR.defaultBlockState());
			TransformStack.of(frame.getTransforms())
				.scale(1 - 1 / 512f);
			add(out, frame, light);

			for (int side : Iterate.positiveAndNegative) {
				SuperByteBuffer wheels =
					CachedBufferer.partial(AllPartialModels.SMALL_BOGEY_WHEELS, Blocks.AIR.defaultBlockState());
				TransformStack.of(wheels.getTransforms())
					.translate(0, 12 / 16f, side)
					.rotateXDegrees(wheelAngle);
				add(out, wheels, light);
			}
		}
	}

	public static class Large extends StandardBogeyRenderer {
		public static final float BELT_RADIUS_PX = 5f;
		public static final float BELT_RADIUS_IN_UV_SPACE = BELT_RADIUS_PX / 16f;

		@Override
		public void extract(CompoundTag bogeyData, float wheelAngle, float partialTick, int light,
			boolean inContraption, List<Part> out) {
			super.extract(bogeyData, wheelAngle, partialTick, light, inContraption, out);

			for (int i : Iterate.zeroAndOne) {
				SuperByteBuffer secondaryShaft = CachedBuffers.block(AllBlocks.SHAFT.getDefaultState()
					.setValue(ShaftBlock.AXIS, Direction.Axis.X));
				TransformStack.of(secondaryShaft.getTransforms())
					.translate(-.5f, .25f, .5f + i * -2)
					.center()
					.rotateXDegrees(wheelAngle)
					.uncenter();
				add(out, secondaryShaft, light);
			}

			SuperByteBuffer drive = CachedBufferer.partial(AllPartialModels.BOGEY_DRIVE, Blocks.AIR.defaultBlockState());
			TransformStack.of(drive.getTransforms())
				.scale(1 - 1 / 512f);
			add(out, drive, light);

			float spriteSize = AllSpriteShifts.BOGEY_BELT.getTarget()
				.getV1()
				- AllSpriteShifts.BOGEY_BELT.getTarget()
					.getV0();

			float scroll = BELT_RADIUS_IN_UV_SPACE * Mth.DEG_TO_RAD * wheelAngle;
			scroll = scroll - Mth.floor(scroll);
			scroll = scroll * spriteSize * 0.5f;

			SuperByteBuffer belt =
				CachedBufferer.partial(AllPartialModels.BOGEY_DRIVE_BELT, Blocks.AIR.defaultBlockState());
			TransformStack.of(belt.getTransforms())
				.scale(1 - 1 / 512f);
			add(out, belt.shiftUVScrolling(AllSpriteShifts.BOGEY_BELT, scroll), light);

			SuperByteBuffer piston =
				CachedBufferer.partial(AllPartialModels.BOGEY_PISTON, Blocks.AIR.defaultBlockState());
			TransformStack.of(piston.getTransforms())
				.translate(0, 0, 1 / 4f * Math.sin(AngleHelper.rad(wheelAngle)));
			add(out, piston, light);

			SuperByteBuffer wheels =
				CachedBufferer.partial(AllPartialModels.LARGE_BOGEY_WHEELS, Blocks.AIR.defaultBlockState());
			TransformStack.of(wheels.getTransforms())
				.translate(0, 1, 0)
				.rotateXDegrees(wheelAngle);
			add(out, wheels, light);

			SuperByteBuffer pin = CachedBufferer.partial(AllPartialModels.BOGEY_PIN, Blocks.AIR.defaultBlockState());
			TransformStack.of(pin.getTransforms())
				.translate(0, 1, 0)
				.rotateXDegrees(wheelAngle)
				.translate(0, 1 / 4f, 0)
				.rotateXDegrees(-wheelAngle);
			add(out, pin, light);
		}
	}
}
