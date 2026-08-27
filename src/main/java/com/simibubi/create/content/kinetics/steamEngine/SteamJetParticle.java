package com.simibubi.create.content.kinetics.steamEngine;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SteamJetParticle extends SimpleAnimatedParticle {

	private float yaw, pitch;

	protected SteamJetParticle(ClientLevel world, SteamJetParticleData data, double x, double y, double z, double dx,
		double dy, double dz, SpriteSet sprite) {
		super(world, x, y, z, sprite, world.getRandom().nextFloat() * .5f);
		xd = 0;
		yd = 0;
		zd = 0;
		gravity = 0;
		quadSize = .375f;
		setLifetime(21);
		setPos(x, y, z);
		roll = oRoll = world.getRandom().nextFloat() * Mth.PI;
		yaw = (float) Mth.atan2(dx, dz) - Mth.PI;
		pitch = (float) Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) - Mth.PI / 2;
		this.setSpriteFromAge(sprite);
	}

	@Override
	public SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.OPAQUE;
	}

	/**
	 * The jet is four quads fanned around its own axis rather than one facing the camera.
	 * <p>
	 * 26.2 collects particles into a render state instead of writing them to a buffer, and a quad's
	 * orientation travels as a rotation, so the four are handed over one at a time with the axis and
	 * spin baked into each rotation.
	 */
	@Override
	public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
		Vec3 cameraPos = camera.position();
		float x = (float) (Mth.lerp(partialTickTime, this.xo, this.x) - cameraPos.x());
		float y = (float) (Mth.lerp(partialTickTime, this.yo, this.y) - cameraPos.y());
		float z = (float) (Mth.lerp(partialTickTime, this.zo, this.z) - cameraPos.z());
		float spin = Mth.lerp(partialTickTime, this.oRoll, this.roll);

		for (int i = 0; i < 4; i++) {
			Quaternionf rotation = Axis.YP.rotation(yaw);
			rotation.mul(Axis.XP.rotation(pitch));
			rotation.mul(Axis.YP.rotation(spin + Mth.PI / 2 * i + roll));
			extractRotatedQuad(particleTypeRenderState, rotation, x, y, z, partialTickTime);
		}
	}

	@Override
	public int getLightCoords(float partialTick) {
		BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
		return this.level.hasChunkAt(blockpos) ? LightCoordsUtil.getLightCoords(level, blockpos) : 0;
	}

	public static class Factory implements ParticleProvider<SteamJetParticleData> {
		private final SpriteSet spriteSet;

		public Factory(SpriteSet animatedSprite) {
			this.spriteSet = animatedSprite;
		}

		public Particle createParticle(SteamJetParticleData data, ClientLevel worldIn, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
			return new SteamJetParticle(worldIn, data, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

}
