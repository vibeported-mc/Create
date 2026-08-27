package com.simibubi.create.content.trains.bogey;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.nbt.CompoundTag;

/**
 * Draws a bogey of one size, for both the block entity in the world and the carriage on a track.
 * <p>
 * Minecraft 26.2 splits rendering into an extract phase, which reads whatever the geometry depends
 * on, and a submit phase that queues it and may run on another thread. A bogey is entirely made of
 * cached partial models whose transforms are their own, so extraction collects them as
 * {@link Part}s and submission just replays them under whatever pose the caller has set up.
 */
public interface BogeyRenderer {

	/**
	 * One piece of bogey geometry, already carrying its own transform.
	 */
	record Part(SuperByteBufferRenderState buffer, RenderType renderType) {
	}

	void extract(CompoundTag bogeyData, float wheelAngle, float partialTick, int light, boolean inContraption,
		List<Part> out);

	static void submit(List<Part> parts, PoseStack poseStack, SubmitNodeCollector queue) {
		for (Part part : parts)
			part.buffer()
				.submit(poseStack, part.renderType(), queue);
	}

}
