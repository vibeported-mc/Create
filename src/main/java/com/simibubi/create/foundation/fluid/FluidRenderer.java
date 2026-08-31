package com.simibubi.create.foundation.fluid;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class FluidRenderer {
	/**
	 * Queue a fluid stream for drawing. The vertex work still happens in
	 * {@link #renderFluidStream(FluidStack, Direction, float, float, boolean, VertexConsumer, PoseStack, int)},
	 * just later, once the queue hands back a consumer.
	 */
	public static void submitFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, OrderedSubmitNodeCollector queue, PoseStack ms, int light) {
		queue.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), (pose, consumer) -> {
			// The callback hands back a resolved Pose, while the vertex code walks a stack of its own.
			PoseStack local = new PoseStack();
			local.last()
				.set(pose);
			renderFluidStream(fluidStack, direction, radius, progress, inbound, consumer, local, light);
		});
	}

	public static void renderFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, VertexConsumer builder, PoseStack ms, int light) {
		Fluid fluid = fluidStack.getFluid();
		FluidType fluidAttributes = fluid.getFluidType();
		TextureAtlasSprite flowTexture = FluidAppearance.flowingTexture(fluidStack);
		TextureAtlasSprite stillTexture = FluidAppearance.stillTexture(fluidStack);

		int color = FluidAppearance.tintColor(fluidStack);
		int blockLightIn = (light >> 4) & 0xF;
		int luminosity = Math.max(blockLightIn, fluidAttributes.getLightLevel(fluidStack));
		light = (light & 0xF00000) | luminosity << 4;

		if (inbound)
			direction = direction.getOpposite();

		var msr = TransformStack.of(ms);
		ms.pushPose();
		msr.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(direction))
			.rotateXDegrees(direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 270)
			.uncenter();
		ms.translate(.5, 0, .5);

		float h = radius;
		float hMin = -radius;
		float hMax = radius;
		float y = inbound ? 1 : .5f;
		float yMin = y - Mth.clamp(progress * .5f, 0, 1);
		float yMax = y;

		for (int i = 0; i < 4; i++) {
			ms.pushPose();
			renderFlowingTiledFace(Direction.SOUTH, hMin, yMin, hMax, yMax, h, builder, ms, light, color, flowTexture);
			ms.popPose();
			msr.rotateYDegrees(90);
		}

		if (progress != 1)
			FluidRenderHelper.renderStillTiledFace(Direction.DOWN, hMin, hMin, hMax, hMax, yMin, builder, ms.last(), light, color, stillTexture);

		ms.popPose();
	}

	public static void renderFlowingTiledFace(Direction dir, float left, float down, float right, float up,
		float depth, VertexConsumer builder, PoseStack ms, int light, int color, TextureAtlasSprite texture) {
		FluidRenderHelper.renderTiledFace(dir, left, down, right, up, depth, builder, ms.last(), light, color, texture, 0.5f);
	}

}
