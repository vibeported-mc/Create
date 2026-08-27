package com.simibubi.create.content.contraptions.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * One movement behaviour's contribution to a contraption, extracted and ready to queue.
 * <p>
 * Minecraft 26.2 splits rendering into an extract phase, which may read the contraption and its
 * virtual level, and a submit phase, which may not. Actors extract into these, and everything an
 * instance holds must already be a value rather than a view onto the contraption.
 */
@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface ActorGeometry {

	void submit(PoseStack ms, SubmitNodeCollector queue);

	/**
	 * Capture where the actor sits within the contraption, so it can be replayed later under
	 * whatever pose the contraption entity ends up at.
	 * <p>
	 * During extraction the contraption's view-projection is still identity, so what this captures is
	 * exactly the local transform the actor built up.
	 */
	static ActorGeometry at(PoseStack local, ActorGeometry inner) {
		Matrix4f captured = new Matrix4f(local.last()
			.pose());
		return (ms, queue) -> {
			ms.pushPose();
			ms.last()
				.pose()
				.mul(captured);
			inner.submit(ms, queue);
			ms.popPose();
		};
	}

	/**
	 * A buffer placed at the actor's local transform. The buffer is extracted here, since it is
	 * pooled and would otherwise be handed to another renderer before this is drawn.
	 */
	static ActorGeometry of(PoseStack local, SuperByteBuffer buffer, RenderType renderType) {
		return at(local, of(buffer, renderType));
	}

	static ActorGeometry of(SuperByteBuffer buffer, RenderType renderType) {
		var state = buffer.extractRenderState();
		return (ms, queue) -> state.submit(ms, renderType, queue);
	}

}
