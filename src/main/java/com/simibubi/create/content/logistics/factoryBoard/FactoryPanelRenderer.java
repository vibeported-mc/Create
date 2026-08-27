package com.simibubi.create.content.logistics.factoryBoard;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class FactoryPanelRenderer
	extends SmartBlockEntityRenderer<FactoryPanelBlockEntity, FactoryPanelRenderer.PanelRenderState> {

	public static class PanelRenderState extends SmartRenderState {
		/** Bulbs and connection sprites, each already carrying its own transform. */
		public final List<Part> parts = new ArrayList<>();
	}

	public record Part(SuperByteBufferRenderState buffer, RenderType renderType) {
	}


	public FactoryPanelRenderer(Context context) {
		super(context);
	}

	@Override
	public PanelRenderState createRenderState() {
		return new PanelRenderState();
	}

	@Override
	protected void extractSafe(FactoryPanelBlockEntity be, PanelRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.parts.clear();

		for (FactoryPanelBehaviour behaviour : be.panels.values()) {
			if (!behaviour.isActive())
				continue;
			if (behaviour.getAmount() > 0)
				extractBulb(behaviour, partialTicks, state.parts, state.lightCoords);
			for (FactoryPanelConnection connection : behaviour.targetedBy.values())
				extractPath(behaviour, connection, partialTicks, state.parts, state.lightCoords);
			for (FactoryPanelConnection connection : behaviour.targetedByLinks.values())
				extractPath(behaviour, connection, partialTicks, state.parts, state.lightCoords);
		}
	}

	@Override
	protected void submitSafe(PanelRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		for (Part part : state.parts)
			part.buffer()
				.submit(ms, part.renderType(), queue);
	}

	public static void extractBulb(FactoryPanelBehaviour behaviour, float partialTicks, List<Part> out, int light) {
		BlockState blockState = behaviour.blockEntity.getBlockState();

		float xRot = FactoryPanelBlock.getXRot(blockState) + Mth.PI / 2;
		float yRot = FactoryPanelBlock.getYRot(blockState);
		float glow = behaviour.bulb.getValue(partialTicks);

		boolean missingAddress = behaviour.isMissingAddress();
		PartialModel partial = behaviour.redstonePowered || missingAddress ? AllPartialModels.FACTORY_PANEL_RED_LIGHT
			: AllPartialModels.FACTORY_PANEL_LIGHT;

		SuperByteBuffer bulb = orient(CachedBufferer.partial(partial, blockState), behaviour, yRot, xRot);
		out.add(new Part(bulb.light(glow > 0.125f ? LightCoordsUtil.FULL_BRIGHT : light)
			.extractRenderState(),
			net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock()));

		if (glow < .125f)
			return;

		glow = (float) (1 - (2 * Math.pow(glow - .75f, 2)));
		glow = Mth.clamp(glow, -1, 1);
		int color = (int) (200 * glow);

		SuperByteBuffer glowBulb = orient(CachedBufferer.partial(partial, blockState), behaviour, yRot, xRot);
		out.add(new Part(glowBulb.light(LightCoordsUtil.FULL_BRIGHT)
			.color(color, color, color, 255)
			.extractRenderState(), RenderTypes.additive()));
	}

	public static void extractPath(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection,
		float partialTicks, List<Part> out, int light) {
		BlockState blockState = behaviour.blockEntity.getBlockState();
		List<Direction> path = connection.getPath(behaviour.getWorld(), blockState, behaviour.getPanelPosition());

		float xRot = FactoryPanelBlock.getXRot(blockState) + Mth.PI / 2;
		float yRot = FactoryPanelBlock.getYRot(blockState);
		float glow = behaviour.bulb.getValue(partialTicks);

		FactoryPanelSupportBehaviour sbe = FactoryPanelBehaviour.linkAt(behaviour.getWorld(), connection);
		boolean displayLinkMode = sbe != null && sbe.blockEntity instanceof DisplayLinkBlockEntity;
		boolean redstoneLinkMode = sbe != null && sbe.blockEntity instanceof RedstoneLinkBlockEntity;
		boolean pathReversed = sbe != null && !sbe.isOutput();

		int color = 0;
		float yOffset = 0;
		boolean success = connection.success;
		boolean dots = false;

		if (displayLinkMode) {
			// Display status
			color = 0x3C9852;
			dots = true;

		} else if (redstoneLinkMode) {
			// Link status
			color = pathReversed ? (behaviour.count == 0 ? 0x888898 : behaviour.satisfied ? 0xEF0000 : 0x580101)
				: (behaviour.redstonePowered ? 0xEF0000 : 0x580101);
			yOffset = 0.5f;

		} else {
			// Regular ingredient status
			color = behaviour.getIngredientStatusColor();

			yOffset = 1;
			yOffset += behaviour.promisedSatisfied ? 1 : behaviour.satisfied ? 0 : 2;

			if (!behaviour.redstonePowered && !behaviour.waitingForNetwork && glow > 0 && !behaviour.satisfied) {
				float p = (1 - (1 - glow) * (1 - glow));
				color = Color.mixColors(color, success ? 0xEAF2EC : 0xE5654B, p);
				if (!behaviour.satisfied && !behaviour.promisedSatisfied)
					yOffset += (success ? 1 : 2) * p;
			}
		}

		float currentX = 0;
		float currentZ = 0;

		for (int i = 0; i < path.size(); i++) {
			Direction direction = path.get(i);

			if (!pathReversed) {
				currentX += direction.getStepX() * .5;
				currentZ += direction.getStepZ() * .5;
			}

			boolean isArrowSegment = pathReversed ? i == path.size() - 1 : i == 0;
			PartialModel partial = (dots ? AllPartialModels.FACTORY_PANEL_DOTTED
				: isArrowSegment ? AllPartialModels.FACTORY_PANEL_ARROWS : AllPartialModels.FACTORY_PANEL_LINES)
					.get(pathReversed ? direction : direction.getOpposite());
			SuperByteBuffer connectionSprite = CachedBufferer.partial(partial, blockState);
			TransformStack.of(connectionSprite.getTransforms())
				.rotateCentered(yRot, Direction.UP)
				.rotateCentered(xRot, Direction.EAST)
				.rotateCentered(Mth.PI, Direction.UP)
				.translate(behaviour.slot.xOffset * .5 + .25, 0, behaviour.slot.yOffset * .5 + .25)
				.translate(currentX, (yOffset + (direction.get2DDataValue() % 2) * 0.125f) / 512f, currentZ);

			if (!displayLinkMode && !redstoneLinkMode && !behaviour.isMissingAddress() && !behaviour.waitingForNetwork
				&& !behaviour.satisfied && !behaviour.redstonePowered)
				connectionSprite.shiftUV(AllSpriteShifts.FACTORY_PANEL_CONNECTIONS);

			out.add(new Part(connectionSprite.color(color)
				.light(light)
				.extractRenderState(),
				net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock()));

			if (pathReversed) {
				currentX += direction.getStepX() * .5;
				currentZ += direction.getStepZ() * .5;
			}
		}
	}

	/**
	 * Bulbs sit flat against whichever face the panel is on, offset into their slot on the board.
	 */
	private static SuperByteBuffer orient(SuperByteBuffer buffer, FactoryPanelBehaviour behaviour, float yRot,
		float xRot) {
		TransformStack.of(buffer.getTransforms())
			.rotateCentered(yRot, Direction.UP)
			.rotateCentered(xRot, Direction.EAST)
			.rotateCentered(Mth.PI, Direction.UP)
			.translate(behaviour.slot.xOffset * .5, 0, behaviour.slot.yOffset * .5);
		return buffer;
	}

}
