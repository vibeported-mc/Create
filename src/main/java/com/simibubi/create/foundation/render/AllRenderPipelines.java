package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
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
	 * The glowing shader ignores the diffuse light direction, sampling the lightmap itself instead of
	 * reusing the entity snippet.
	 * <p>
	 * There are two of these because a pipeline is only valid in the pass it was built for: the level
	 * pass draws through the matrices and fog snippet, while an inventory draws item geometry and
	 * silently drops anything else. Both snippets already declare the samplers this shader reads.
	 */
	public static final RenderPipeline.Snippet GLOWING_SNIPPET = glowingSnippet(RenderPipelines.MATRICES_FOG_SNIPPET);

	public static final RenderPipeline.Snippet GUI_GLOWING_SNIPPET = glowingSnippet(RenderPipelines.ITEM_SNIPPET);

	private static RenderPipeline.Snippet glowingSnippet(RenderPipeline.Snippet base) {
		RenderPipeline.Builder builder = RenderPipeline.builder(base)
			.withVertexShader(GLOWING_ID)
			.withFragmentShader(GLOWING_ID)
			.withVertexBinding(0, DefaultVertexFormat.ENTITY)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withDepthStencilState(DepthStencilState.DEFAULT);
		if (base == RenderPipelines.MATRICES_FOG_SNIPPET)
			builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2);
		return builder.buildSnippet();
	}

	public static final RenderPipeline ADDITIVE = pipeline("additive",
		RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
			.withCull(false));

	public static final RenderPipeline GLOWING = pipeline("glowing", glowing(GLOWING_SNIPPET));

	public static final RenderPipeline GLOWING_TRANSLUCENT = pipeline("glowing_translucent",
		RenderPipeline.builder(GLOWING_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)));

	public static final RenderPipeline GUI_GLOWING = pipeline("gui_glowing", glowing(GUI_GLOWING_SNIPPET));

	public static final RenderPipeline GUI_GLOWING_TRANSLUCENT = pipeline("gui_glowing_translucent",
		RenderPipeline.builder(GUI_GLOWING_SNIPPET)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)));

	private static RenderPipeline.Builder glowing(RenderPipeline.Snippet snippet) {
		return RenderPipeline.builder(snippet)
			.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA,
				BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)));
	}

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
		event.registerPipeline(GUI_GLOWING);
		event.registerPipeline(GUI_GLOWING_TRANSLUCENT);
	}

}
