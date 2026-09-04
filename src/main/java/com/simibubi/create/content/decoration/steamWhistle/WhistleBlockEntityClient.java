package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The whistle's sound, which only a client makes.
 * <p>
 * This was {@code WhistleBlockEntity#tickAudio}, already called through
 * {@code executeOnClientOnly}. That was enough while {@code @OnlyIn(Dist.CLIENT)} stripped the
 * method; in 26.2 the body stays, and handing a {@link WhistleSoundInstance} to the sound manager
 * makes the JVM resolve it -- and it descends from a client class -- when it verifies the block
 * entity. So the block entity could not be loaded on a dedicated server, and Create failed to
 * register its block entities. The body is unchanged; only where it lives is.
 */
public class WhistleBlockEntityClient {

	public static void tickAudio(WhistleBlockEntity be, WhistleSize size, boolean powered) {
		if (!powered) {
			if (be.soundInstance != null) {
				be.soundInstance.fadeOut();
				be.soundInstance = null;
			}
			return;
		}

		float f = (float) Math.pow(2, -be.pitch / 12.0);
		boolean particle = be.getLevel().getGameTime() % 8 == 0;
		Vec3 eyePosition = Minecraft.getInstance().getCameraEntity().getEyePosition();
		float maxVolume =
			(float) Mth.clamp((64 - eyePosition.distanceTo(Vec3.atCenterOf(be.getBlockPos()))) / 64, 0, 1);

		if (be.soundInstance == null || be.soundInstance.isStopped() || be.soundInstance.getOctave() != size) {
			Minecraft.getInstance()
				.getSoundManager()
				.play(be.soundInstance = new WhistleSoundInstance(size, be.getBlockPos()));
			AllSoundEvents.WHISTLE_CHIFF.playAt(be.getLevel(), be.getBlockPos(), maxVolume * .175f,
				size == WhistleSize.SMALL ? f + .75f : f, false);
			particle = true;
		}

		be.soundInstance.keepAlive();
		be.soundInstance.setPitch(f);

		if (!particle)
			return;

		Direction facing = be.getBlockState()
			.getOptionalValue(WhistleBlock.FACING)
			.orElse(Direction.SOUTH);
		float angle = 180 + AngleHelper.horizontalAngle(facing);
		Vec3 sizeOffset = VecHelper.rotate(new Vec3(0, -0.4f, 1 / 16f * size.ordinal()), angle, Axis.Y);
		Vec3 offset = VecHelper.rotate(new Vec3(0, 1, 0.75f), angle, Axis.Y);
		Vec3 v = offset.scale(.45f)
			.add(sizeOffset)
			.add(Vec3.atCenterOf(be.getBlockPos()));
		Vec3 m = offset.subtract(Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(.75f));
		be.getLevel()
			.addParticle(new SteamJetParticleData(1), v.x, v.y, v.z, m.x, m.y, m.z);
	}

	private WhistleBlockEntityClient() {}
}
