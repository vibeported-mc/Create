package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.simibubi.create.Create;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Minecraft 26.2 replaced the old shader/render-state shards with {@link RenderPipeline}s, which
 * describe the whole GPU state up front and are registered once rather than looked up by name. The
 * blend modes and the glowing shader that Create used to configure per render type live here now;
 * {@link RenderTypes} composes them with textures into render types.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Create.ID)
public class AllRenderPipelines {

	public static final Identifier GLOWING_ID = Create.asResource("core/glowing_shader");

	/**
	 * The glowing shader is Minecraft's own item shader with the diffuse mixing and the overlay taken
	 * out, so it needs only the texture and lightmap samplers - the lighting bind group vanilla's item
	 * snippet adds is exactly what this render type exists to skip.
	 * <p>
	 * One snippet serves both the level and an inventory. They looked like they needed a pipeline
	 * each, but the two passes agree on everything a pipeline fixes: 26.2 draws item icons into an
	 * offscreen atlas whose depth buffer is reversed the same way the level's is, so
	 * {@link DepthStencilState#DEFAULT} is right for both.
	 */
	public static final RenderPipeline.Snippet GLOWING_SNIPPET = RenderPipeline
		.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withVertexShader(GLOWING_ID)
		.withFragmentShader(GLOWING_ID)
		.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
		.withVertexBinding(0, DefaultVertexFormat.ENTITY)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.withDepthStencilState(DepthStencilState.DEFAULT)
		.buildSnippet();

	public static final RenderPipeline ADDITIVE = pipeline("additive",
		RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
			.withCull(false));

	/**
	 * The solid half draws opaque. It is the lit core of a model whose glow is the separate
	 * translucent half, and blending it turns that core into a window.
	 */
	public static final RenderPipeline GLOWING = pipeline("glowing", RenderPipeline.builder(GLOWING_SNIPPET));

	public static final RenderPipeline GLOWING_TRANSLUCENT = pipeline("glowing_translucent",
		RenderPipeline.builder(GLOWING_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)));

	/**
	 * Additive blending over the particle snippet, for Create's own particle group.
	 */
	public static final RenderPipeline ADDITIVE_PARTICLE = pipeline("additive_particle",
		RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)));

	private static RenderPipeline pipeline(String id, RenderPipeline.Builder builder) {
		return builder.withLocation(Create.asResource("pipeline/" + id))
			.build();
	}

	@SubscribeEvent
	static void registerPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(ADDITIVE);
		event.registerPipeline(ADDITIVE_PARTICLE);
		event.registerPipeline(GLOWING);
		event.registerPipeline(GLOWING_TRANSLUCENT);
	}

}
