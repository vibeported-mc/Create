package com.simibubi.create.content.trains;

import java.util.Set;

import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The extracted state for {@link CubeParticle}.
 * <p>
 * 26.2 no longer lets a particle write its own vertices; the engine collects a render state per
 * group and the quad feature renderer turns it into geometry. A cube is six quads in the same
 * vertex format the quad renderer already uses, so this extends the vanilla state and swaps out the
 * geometry rather than needing a feature renderer of its own.
 */
@OnlyIn(Dist.CLIENT)
public class CubeParticleRenderState extends QuadParticleRenderState {

	private final FloatArrayList positions = new FloatArrayList();
	private final FloatArrayList scales = new FloatArrayList();
	private final IntArrayList colors = new IntArrayList();
	private final IntArrayList lights = new IntArrayList();

	public void addCube(float x, float y, float z, float scale, int color, int lightCoords) {
		positions.add(x);
		positions.add(y);
		positions.add(z);
		scales.add(scale);
		colors.add(color);
		lights.add(lightCoords);
	}

	@Override
	public Set<SingleQuadParticle.Layer> layers() {
		return Set.of(CubeParticleGroup.LAYER);
	}

	@Override
	public boolean isEmpty() {
		return scales.isEmpty();
	}

	@Override
	public void buildLayer(SingleQuadParticle.Layer layer, VertexConsumer builder) {
		if (layer != CubeParticleGroup.LAYER)
			return;

		for (int i = 0; i < scales.size(); i++) {
			float x = positions.getFloat(i * 3);
			float y = positions.getFloat(i * 3 + 1);
			float z = positions.getFloat(i * 3 + 2);
			float scale = scales.getFloat(i);
			int color = colors.getInt(i);
			int light = lights.getInt(i);

			for (int face = 0; face < 6; face++) {
				for (int corner = 0; corner < 4; corner++) {
					Vec3 vec = CubeParticle.CUBE[face * 4 + corner].scale(-scale);
					builder.addVertex((float) vec.x + x, (float) vec.y + y, (float) vec.z + z)
						.setUv((float) corner / 2, corner % 2)
						.setColor(color)
						.setLight(light);
				}
			}
		}
	}

	@Override
	public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (!isEmpty())
			submitNodeCollector.submitQuadParticleGroup(this);
	}

	@Override
	public void clear() {
		positions.clear();
		scales.clear();
		colors.clear();
		lights.clear();
	}

}
