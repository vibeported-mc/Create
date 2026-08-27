package com.simibubi.create.foundation.render;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.simibubi.create.Create;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Minecraft 26.2 builds render types from a {@link RenderPipeline} plus a {@link RenderSetup} that
 * names the textures and samplers, replacing the shader/transparency/lightmap shards these types
 * used to be assembled from. The pipelines themselves are in {@link AllRenderPipelines}.
 */
public class RenderTypes {

	private static final Supplier<GpuSampler> BLOCK_SHEET_MIPPED = () -> RenderSystem.getSamplerCache()
		.getClampToEdge(FilterMode.NEAREST, true);

	private static final RenderType ENTITY_SOLID_BLOCK_MIPPED = RenderType.create(
		createLayerName("entity_solid_block_mipped"), RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, BLOCK_SHEET_MIPPED)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	private static final RenderType ENTITY_CUTOUT_BLOCK_MIPPED = RenderType.create(
		createLayerName("entity_cutout_block_mipped"), RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, BLOCK_SHEET_MIPPED)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	private static final RenderType ENTITY_TRANSLUCENT_BLOCK_MIPPED = RenderType.create(
		createLayerName("entity_translucent_block_mipped"), RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, BLOCK_SHEET_MIPPED)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.sortOnUpload()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	private static final RenderType ADDITIVE = RenderType.create(createLayerName("additive"),
		RenderSetup.builder(AllRenderPipelines.ADDITIVE)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.sortOnUpload()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	private static final RenderType ITEM_GLOWING_SOLID = RenderType.create(createLayerName("item_glowing_solid"),
		RenderSetup.builder(AllRenderPipelines.GLOWING)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	private static final RenderType ITEM_GLOWING_TRANSLUCENT = RenderType.create(
		createLayerName("item_glowing_translucent"), RenderSetup.builder(AllRenderPipelines.GLOWING_TRANSLUCENT)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.sortOnUpload()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup());

	/** The chain texture is tiled along the strand, so it samples with repeat rather than clamp. */
	public static final Supplier<GpuSampler> CHAIN_SAMPLER = () -> RenderSystem.getSamplerCache()
		.getRepeat(FilterMode.NEAREST, true);

	private static final Function<Identifier, RenderType> CHAIN = Util.memoize(texture -> RenderType
		.create("chain_conveyor_chain", RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK)
			.withTexture("Sampler0", texture, CHAIN_SAMPLER)
			.useLightmap()
			.useOverlay()
			.sortOnUpload()
			.setOutline(OutlineProperty.AFFECTS_OUTLINE)
			.createRenderSetup()));

	public static RenderType entitySolidBlockMipped() {
		return ENTITY_SOLID_BLOCK_MIPPED;
	}

	public static RenderType entityCutoutBlockMipped() {
		return ENTITY_CUTOUT_BLOCK_MIPPED;
	}

	public static RenderType entityTranslucentBlockMipped() {
		return ENTITY_TRANSLUCENT_BLOCK_MIPPED;
	}

	public static RenderType additive() {
		return ADDITIVE;
	}

	public static BiFunction<Identifier, Boolean, RenderType> TRAIN_MAP = Util.memoize(RenderTypes::getTrainMap);

	private static RenderType getTrainMap(Identifier locationIn, boolean linearFiltering) {
		return RenderType.create("create_train_map", RenderSetup.builder(RenderPipelines.TEXT)
			.withTexture("Sampler0", locationIn, () -> RenderSystem.getSamplerCache()
				.getClampToEdge(linearFiltering ? FilterMode.LINEAR : FilterMode.NEAREST))
			.useLightmap()
			.sortOnUpload()
			.createRenderSetup());
	}

	public static RenderType itemGlowingSolid() {
		return ITEM_GLOWING_SOLID;
	}

	public static RenderType itemGlowingTranslucent() {
		return ITEM_GLOWING_TRANSLUCENT;
	}

	public static RenderType chain(Identifier pLocation) {
		return CHAIN.apply(pLocation);
	}

	private static String createLayerName(String name) {
		return Create.ID + ":" + name;
	}

	private RenderTypes() {
	}

}
