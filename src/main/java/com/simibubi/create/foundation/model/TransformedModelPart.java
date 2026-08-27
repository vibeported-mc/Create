package com.simibubi.create.foundation.model;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;

/**
 * A model part whose quads are rewritten on the way out.
 * <p>
 * Minecraft 26.2 collects a model into parts rather than asking it for quads face by face, so the
 * models that used to edit the list returned by {@code getQuads} wrap each collected part in one of
 * these instead.
 */
public class TransformedModelPart implements BlockStateModelPart {

	/**
	 * Rewrites one quad into any number of quads, appending them to {@code out}. Appending nothing
	 * drops the quad.
	 */
	@FunctionalInterface
	public interface QuadTransform {
		void apply(BakedQuad quad, @Nullable Direction cullFace, List<BakedQuad> out);
	}

	private final BlockStateModelPart delegate;
	private final QuadTransform transform;

	public TransformedModelPart(BlockStateModelPart delegate, QuadTransform transform) {
		this.delegate = delegate;
		this.transform = transform;
	}

	/**
	 * Wraps every part added to {@code parts} from index {@code from} onwards.
	 */
	public static void wrapFrom(List<BlockStateModelPart> parts, int from, QuadTransform transform) {
		for (int i = from; i < parts.size(); i++)
			parts.set(i, new TransformedModelPart(parts.get(i), transform));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable Direction direction) {
		List<BakedQuad> quads = delegate.getQuads(direction);
		List<BakedQuad> result = new ArrayList<>(quads.size());
		for (BakedQuad quad : quads)
			transform.apply(quad, direction, result);
		return result;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useAmbientOcclusion() {
		return delegate.useAmbientOcclusion();
	}

	@Override
	public TriState ambientOcclusion() {
		return delegate.ambientOcclusion();
	}

	@Override
	public Material.Baked particleMaterial() {
		return delegate.particleMaterial();
	}

	@Override
	public int materialFlags() {
		return delegate.materialFlags();
	}

}
