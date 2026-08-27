package com.simibubi.create.foundation.model;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Reads and rebuilds the geometry of a {@link BakedQuad}.
 * <p>
 * Minecraft 26.2 turned baked quads into a record of four positions, four packed UVs and a shared
 * material, in place of the packed {@code int[]} of vertex data Create used to poke at. Quads are
 * immutable now, so the setters here take a working copy and hand back a new quad.
 */
public final class BakedQuadHelper {

	public static final int VERTEX_COUNT = BakedQuad.VERTEX_COUNT;
	public static final int MAX_LIGHT_EMISSION = 15;

	private BakedQuadHelper() {}

	/**
	 * The quad's four corners, in a mutable array to edit before rebuilding.
	 */
	public static Vector3f[] positions(BakedQuad quad) {
		Vector3f[] positions = new Vector3f[VERTEX_COUNT];
		for (int vertex = 0; vertex < VERTEX_COUNT; vertex++)
			positions[vertex] = new Vector3f(quad.position(vertex));
		return positions;
	}

	/**
	 * The quad's four packed UVs, in a mutable array to edit before rebuilding.
	 */
	public static long[] uvs(BakedQuad quad) {
		long[] uvs = new long[VERTEX_COUNT];
		for (int vertex = 0; vertex < VERTEX_COUNT; vertex++)
			uvs[vertex] = quad.packedUV(vertex);
		return uvs;
	}

	public static Vec3 getXYZ(Vector3fc[] positions, int vertex) {
		Vector3fc pos = positions[vertex];
		return new Vec3(pos.x(), pos.y(), pos.z());
	}

	public static void setXYZ(Vector3f[] positions, int vertex, Vec3 xyz) {
		positions[vertex].set((float) xyz.x, (float) xyz.y, (float) xyz.z);
	}

	public static float getU(long[] uvs, int vertex) {
		return UVPair.unpackU(uvs[vertex]);
	}

	public static float getV(long[] uvs, int vertex) {
		return UVPair.unpackV(uvs[vertex]);
	}

	public static void setU(long[] uvs, int vertex, float u) {
		uvs[vertex] = UVPair.pack(u, UVPair.unpackV(uvs[vertex]));
	}

	public static void setV(long[] uvs, int vertex, float v) {
		uvs[vertex] = UVPair.pack(UVPair.unpackU(uvs[vertex]), v);
	}

	/**
	 * A copy of {@code quad} with the given geometry.
	 */
	public static BakedQuad withGeometry(BakedQuad quad, Vector3fc[] positions, long[] uvs) {
		return new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3],
			quad.direction(), quad.materialInfo(), quad.bakedNormals(), quad.bakedColors());
	}

	/**
	 * A copy of {@code quad} facing a different way.
	 */
	public static BakedQuad withDirection(BakedQuad quad, Direction direction) {
		return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(),
			quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), direction, quad.materialInfo(), quad.bakedNormals(),
			quad.bakedColors());
	}

	/**
	 * A copy of {@code quad} drawn from a different sprite, keeping the same position within it.
	 */
	public static BakedQuad withSprite(BakedQuad quad, TextureAtlasSprite sprite) {
		BakedQuad.MaterialInfo info = quad.materialInfo();
		return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(),
			quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
			new BakedQuad.MaterialInfo(sprite, info.layer(), info.itemRenderType(), info.tintIndex(), info.shade(),
				info.lightEmission(), info.ambientOcclusion()),
			quad.bakedNormals(), quad.bakedColors());
	}

	/**
	 * A copy of {@code quad} lit at full block light.
	 * <p>
	 * NeoForge's emissivity quad transformer is gone in 26.2; emission is a field of the quad's
	 * material now.
	 */
	public static BakedQuad withMaxEmissivity(BakedQuad quad) {
		BakedQuad.MaterialInfo info = quad.materialInfo();
		if (info.lightEmission() >= MAX_LIGHT_EMISSION)
			return quad;
		return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(),
			quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
			new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(), info.tintIndex(),
				info.shade(), MAX_LIGHT_EMISSION, info.ambientOcclusion()),
			quad.bakedNormals(), quad.bakedColors());
	}

	public static TextureAtlasSprite getSprite(BakedQuad quad) {
		return quad.materialInfo()
			.sprite();
	}

}
