package com.simibubi.create.content.equipment.bell;

import org.joml.Quaternionf;

import com.mojang.math.Axis;

import dev.engine_room.flywheel.lib.util.ShadersModHelper;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class CustomRotationParticle extends SimpleAnimatedParticle {

	protected boolean mirror;
	protected int loopLength;

	public CustomRotationParticle(ClientLevel worldIn, double x, double y, double z, SpriteSet spriteSet, float yAccel) {
		super(worldIn, x, y, z, spriteSet, yAccel);
	}

	public void selectSpriteLoopingWithAge(SpriteSet sprite) {
		int loopFrame = age % loopLength;
		this.setSprite(sprite.get(loopFrame, loopLength));
	}

	public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
		Quaternionf quaternion = new Quaternionf(camera.rotation());
		if (roll != 0.0F) {
			float angle = Mth.lerp(partialTicks, oRoll, roll);
			quaternion.mul(Axis.ZP.rotation(angle));
		}
		return quaternion;
	}

	/**
	 * 26.2 collects a particle into a render state rather than letting it write vertices, so the custom
	 * rotation is handed over with the quad and the mirroring is expressed by swapping the U bounds.
	 */
	@Override
	public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
		extractRotatedQuad(particleTypeRenderState, camera, getCustomRotation(camera, partialTickTime),
			partialTickTime);
	}

	@Override
	protected float getU0() {
		return mirror ? super.getU1() : super.getU0();
	}

	@Override
	protected float getU1() {
		return mirror ? super.getU0() : super.getU1();
	}

	@Override
	public int getLightCoords(float partialTicks) {
		return ShadersModHelper.isShaderPackInUse() ? LightCoordsUtil.pack(12, 15)
			: super.getLightCoords(partialTicks);
	}

}
