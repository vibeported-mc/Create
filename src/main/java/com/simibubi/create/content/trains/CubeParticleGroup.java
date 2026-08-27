package com.simibubi.create.content.trains;

import net.createmod.catnip.api.client.gui.texture.CatnipSpecialTextures;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.render.AllRenderPipelines;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The group train smoke is collected into.
 * <p>
 * 26.2 sorts particles into groups that each extract a render state; a group only exists for a
 * {@link ParticleRenderType} the engine has been told about, which happens through NeoForge's
 * RegisterParticleGroupsEvent in {@link com.simibubi.create.CreateClient}.
 */
@OnlyIn(Dist.CLIENT)
public class CubeParticleGroup extends ParticleGroup<CubeParticle> {

	public static final ParticleRenderType TYPE = new ParticleRenderType(Create.ID + ":cube", "CU");

	/**
	 * Untextured and additive, the way the old render type set up its blend function by hand.
	 */
	public static final SingleQuadParticle.Layer LAYER = new SingleQuadParticle.Layer(true,
		CatnipSpecialTextures.BLANK.getId(), AllRenderPipelines.ADDITIVE_PARTICLE);

	private final CubeParticleRenderState renderState = new CubeParticleRenderState();

	public CubeParticleGroup(ParticleEngine engine) {
		super(engine);
	}

	@Override
	public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
		renderState.clear();
		for (CubeParticle particle : particles)
			particle.extract(renderState, camera, partialTickTime);
		return renderState;
	}

}
