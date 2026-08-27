package com.simibubi.create.content.trains.entity;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import java.util.List;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class CarriageContraptionEntityRenderer
	extends ContraptionEntityRenderer<CarriageContraptionEntity, CarriageContraptionEntityRenderer.CarriageRenderState> {

	public static class CarriageRenderState extends ContraptionRenderState {
		public final List<PlacedBogey> bogeys = new ArrayList<>();

	}

	/** A bogey's geometry together with where it sits under the carriage. */
	public record PlacedBogey(PoseStack placement, List<BogeyRenderer.Part> parts) {
	}


	public CarriageContraptionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(CarriageContraptionEntity entity, Frustum clippingHelper, double cameraX,
		double cameraY, double cameraZ) {
		Carriage carriage = entity.getCarriage();
		if (carriage != null)
			for (CarriageBogey bogey : carriage.bogeys)
				if (bogey != null)
					bogey.couplingAnchors.replace(v -> null);
		return super.shouldRender(entity, clippingHelper, cameraX, cameraY, cameraZ);
	}

	@Override
	public CarriageRenderState createRenderState() {
		return new CarriageRenderState();
	}

	@Override
	public void extractRenderState(CarriageContraptionEntity entity, CarriageRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.bogeys.clear();

		if (!entity.validForRender || entity.firstPositionUpdate) {
			state.skip = true;
			return;
		}
		state.skip = false;

		Carriage carriage = entity.getCarriage();
		if (carriage == null)
			return;

		Vec3 position = entity.getPosition(partialTicks);

		float viewYRot = entity.getViewYRot(partialTicks);
		float viewXRot = entity.getViewXRot(partialTicks);
		int bogeySpacing = carriage.bogeySpacing;

		carriage.bogeys.forEach(bogey -> {
			if (bogey == null)
				return;

			BlockPos bogeyPos = bogey.isLeading ? BlockPos.ZERO
				: BlockPos.ZERO.relative(entity.getInitialOrientation()
					.getCounterClockWise(), bogeySpacing);

			if (!VisualizationManager.supportsVisualization(entity.level()) && !entity.getContraption()
				.isHiddenInPortal(bogeyPos)) {

				// The placement is built here and captured, since the bogey itself must not be read
				// again once submission starts.
				PoseStack placement = new PoseStack();
				translateBogey(placement, bogey, bogeySpacing, viewYRot, viewXRot, partialTicks);

				List<BogeyRenderer.Part> parts = new ArrayList<>();
				bogey.getStyle()
					.extract(bogey.getSize(), partialTicks, getBogeyLightCoords(entity, bogey, partialTicks),
						bogey.wheelAngle.getValue(partialTicks), bogey.bogeyData, true, parts);

				state.bogeys.add(new PlacedBogey(placement, parts));
			}

			bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, bogey.isLeading);
			if (!carriage.isOnTwoBogeys())
				bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, !bogey.isLeading);
		});
	}

	@Override
	public void submit(CarriageRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.skip)
			return;
		super.submit(state, ms, queue, camera);

		for (PlacedBogey bogey : state.bogeys) {
			ms.pushPose();
			ms.last()
				.pose()
				.mul(bogey.placement()
					.last()
					.pose());
			BogeyStyle.submit(bogey.parts(), ms, queue);
			ms.popPose();
		}
	}

	public static void translateBogey(PoseStack ms, CarriageBogey bogey, int bogeySpacing, float viewYRot,
		float viewXRot, float partialTicks) {
		boolean selfUpsideDown = bogey.isUpsideDown();
		boolean leadingUpsideDown = bogey.carriage.leadingBogey().isUpsideDown();
		TransformStack.of(ms)
			.rotateYDegrees(viewYRot + 90)
			.rotateXDegrees(-viewXRot)
			.rotateYDegrees(180)
			.translate(0, 0, bogey.isLeading ? 0 : -bogeySpacing)
			.rotateYDegrees(-180)
			.rotateXDegrees(viewXRot)
			.rotateYDegrees(-viewYRot - 90)
			.rotateYDegrees(bogey.yaw.getValue(partialTicks))
			.rotateXDegrees(bogey.pitch.getValue(partialTicks))
			.translate(0, .5f, 0)
			.rotateZDegrees(selfUpsideDown ? 180 : 0)
			.translateY(selfUpsideDown != leadingUpsideDown ? 2 : 0);
	}

	public static int getBogeyLightCoords(CarriageContraptionEntity entity, CarriageBogey bogey, float partialTicks) {
		var anchorPosition = bogey.getAnchorPosition();

		var lightPos = BlockPos.containing(anchorPosition == null ? entity.getLightProbePosition(partialTicks) : anchorPosition);

		return LightCoordsUtil.pack(entity.level().getBrightness(LightLayer.BLOCK, lightPos),
			entity.level().getBrightness(LightLayer.SKY, lightPos));
	}

}
