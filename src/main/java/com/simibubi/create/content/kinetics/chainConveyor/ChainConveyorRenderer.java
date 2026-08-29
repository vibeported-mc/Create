package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectionStats;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.render.RenderTypes;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorRenderer
	extends KineticBlockEntityRenderer<ChainConveyorBlockEntity, ChainConveyorRenderer.ChainConveyorRenderState> {

	/**
	 * 26.2 gave chains a metal: the block became iron_chain alongside the copper ones, and its texture
	 * moved with it. The old path silently resolves to the missing texture, which is what the strand
	 * between two posts was being drawn with.
	 */
	public static final Identifier CHAIN_LOCATION =
		Identifier.withDefaultNamespace("textures/block/iron_chain.png");
	public static final int MIP_DISTANCE = 48;

	public static class ChainConveyorRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState wheel;
		/** Guards and hanging packages, each already carrying its own transform. */
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
		public final List<ChainSegment> chains = new ArrayList<>();
	}

	/**
	 * A chain strand between two conveyor wheels. Its geometry is generated at submit time, so only
	 * the placement and the two lightmap samples have to survive extraction.
	 */
	public record ChainSegment(Vec3 startOffset, float yaw, float pitch, float animation, float length, int light1,
		int light2, boolean far) {
	}

	public ChainConveyorRenderer(Context context) {
		super(context);
	}

	@Override
	public ChainConveyorRenderState createRenderState() {
		return new ChainConveyorRenderState();
	}

	@Override
	protected void extractSafe(ChainConveyorBlockEntity be, ChainConveyorRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		// The kinetic base skips everything when Flywheel is on, but the chains and packages are not
		// instanced, so they are extracted either way.
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.skip = false;

		state.wheel = null;
		state.parts.clear();
		state.chains.clear();

		BlockPos pos = be.getBlockPos();
		boolean visualized = VisualizationManager.supportsVisualization(be.getLevel());

		extractChains(be, state, cameraPosition, visualized);

		if (visualized)
			return;

		state.wheel = CachedBufferer.partial(AllPartialModels.CHAIN_CONVEYOR_WHEEL, be.getBlockState())
			.light(state.lightCoords)
			.extractRenderState();

		for (ChainConveyorPackage box : be.loopingPackages)
			extractBox(be, state, pos, box, partialTicks);
		for (Entry<BlockPos, List<ChainConveyorPackage>> entry : be.travellingPackages.entrySet())
			for (ChainConveyorPackage box : entry.getValue())
				extractBox(be, state, pos, box, partialTicks);
	}

	@Override
	protected void submitSafe(ChainConveyorRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.model != null)
			state.model.submit(ms, state.renderType, queue);

		for (ChainSegment segment : state.chains) {
			ms.pushPose();
			var chain = TransformStack.of(ms);
			chain.center();
			chain.translate(segment.startOffset());
			chain.rotateYDegrees(segment.yaw());
			chain.rotateXDegrees(90 - segment.pitch());
			chain.rotateYDegrees(45);
			chain.translate(0, 8 / 16f, 0);
			chain.uncenter();

			submitChain(ms, queue, segment.animation(), segment.length(), segment.light1(), segment.light2(),
				segment.far());

			ms.popPose();
		}

		if (state.wheel != null)
			state.wheel.submit(ms, net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock(), queue);

		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock(), queue);
	}

	private void extractBox(ChainConveyorBlockEntity be, ChainConveyorRenderState state, BlockPos pos,
		ChainConveyorPackage box, float partialTicks) {
		if (box.worldPosition == null)
			return;
		if (box.item == null || box.item.isEmpty())
			return;

		ChainConveyorPackagePhysicsData physicsData = box.physicsData(be.getLevel());
		if (physicsData.prevPos == null)
			return;

		Vec3 position = physicsData.prevPos.lerp(physicsData.pos, partialTicks);
		Vec3 targetPosition = physicsData.prevTargetPos.lerp(physicsData.targetPos, partialTicks);
		float yaw = AngleHelper.angleLerp(partialTicks, physicsData.prevYaw, physicsData.yaw);
		Vec3 offset =
			new Vec3(targetPosition.x - pos.getX(), targetPosition.y - pos.getY(), targetPosition.z - pos.getZ());

		BlockPos containingPos = BlockPos.containing(position);
		Level level = be.getLevel();
		BlockState blockState = be.getBlockState();
		int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, containingPos),
			level.getBrightness(LightLayer.SKY, containingPos));

		if (physicsData.modelKey == null) {
			Identifier key = BuiltInRegistries.ITEM.getKey(box.item.getItem());
			if (key == BuiltInRegistries.ITEM.getDefaultKey())
				return;
			physicsData.modelKey = key;
		}

		SuperByteBuffer rigBuffer =
			CachedBufferer.partial(AllPartialModels.PACKAGE_RIGGING.get(physicsData.modelKey), blockState);
		SuperByteBuffer boxBuffer =
			CachedBufferer.partial(AllPartialModels.PACKAGES.get(physicsData.modelKey), blockState);

		Vec3 dangleDiff = VecHelper.rotate(targetPosition.add(0, 0.5, 0)
			.subtract(position), -yaw, Axis.Y);
		float zRot = Mth.wrapDegrees((float) Mth.atan2(-dangleDiff.x, dangleDiff.y) * Mth.RAD_TO_DEG) / 2;
		float xRot = Mth.wrapDegrees((float) Mth.atan2(dangleDiff.z, dangleDiff.y) * Mth.RAD_TO_DEG) / 2;
		zRot = Mth.clamp(zRot, -25, 25);
		xRot = Mth.clamp(xRot, -25, 25);

		for (SuperByteBuffer buf : new SuperByteBuffer[] { rigBuffer, boxBuffer }) {
			var buffer = TransformStack.of(buf.getTransforms());
			buffer.translate(offset);
			buffer.translate(0, 10 / 16f, 0);
			buffer.rotateYDegrees(yaw);

			buffer.rotateZDegrees(zRot);
			buffer.rotateXDegrees(xRot);

			if (physicsData.flipped && buf == rigBuffer)
				buffer.rotateYDegrees(180);

			buffer.uncenter();
			buffer.translate(0, -PackageItem.getHookDistance(box.item) + 7 / 16f, 0);

			state.parts.add(buf.light(light)
				.extractRenderState());
		}
	}

	private void extractChains(ChainConveyorBlockEntity be, ChainConveyorRenderState state, Vec3 cameraPosition,
		boolean visualized) {
		float time = AnimationTickHolder.getRenderTime(be.getLevel()) / (360f / Math.abs(be.getSpeed()));
		time %= 1;
		if (time < 0)
			time += 1;

		float animation = time - 0.5f;

		for (BlockPos blockPos : be.connections) {
			ConnectionStats stats = be.connectionStats.get(blockPos);
			if (stats == null)
				continue;

			Vec3 diff = stats.end()
				.subtract(stats.start());
			float yaw = Mth.RAD_TO_DEG * (float) Mth.atan2(diff.x, diff.z);
			float pitch = Mth.RAD_TO_DEG * (float) Mth.atan2(diff.y, diff.multiply(1, 0, 1)
				.length());

			Level level = be.getLevel();
			BlockPos tilePos = be.getBlockPos();
			Vec3 startOffset = stats.start()
				.subtract(Vec3.atCenterOf(tilePos));

			if (!visualized) {
				SuperByteBuffer guard =
					CachedBufferer.partial(AllPartialModels.CHAIN_CONVEYOR_GUARD, be.getBlockState());
				var guardTransform = TransformStack.of(guard.getTransforms());
				guardTransform.center();
				guardTransform.rotateYDegrees(yaw);
				guardTransform.uncenter();
				state.parts.add(guard.light(state.lightCoords)
					.extractRenderState());
			}

			int light1 = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, tilePos),
				level.getBrightness(LightLayer.SKY, tilePos));
			int light2 = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, tilePos.offset(blockPos)),
				level.getBrightness(LightLayer.SKY, tilePos.offset(blockPos)));

			boolean far = Minecraft.getInstance().level == be.getLevel() && !cameraPosition
				.closerThan(Vec3.atCenterOf(tilePos)
					.add(blockPos.getX() / 2f, blockPos.getY() / 2f, blockPos.getZ() / 2f), MIP_DISTANCE);

			state.chains.add(
				new ChainSegment(startOffset, yaw, pitch, animation, stats.chainLength(), light1, light2, far));
		}
	}

	/**
	 * The chain is raw quads rather than a model, so it goes in as custom geometry and is written
	 * once the queue hands over a consumer.
	 */
	public static void submitChain(PoseStack ms, SubmitNodeCollector queue, float animation, float length, int light1,
		int light2, boolean far) {
		queue.submitCustomGeometry(ms, RenderTypes.chain(CHAIN_LOCATION),
			(pose, consumer) -> renderChain(pose, consumer, animation, length, light1, light2, far));
	}

	public static void renderChain(PoseStack.Pose pose, VertexConsumer vc, float animation, float length, int light1,
		int light2, boolean far) {
		float radius = far ? 1f / 16f : 1.5f / 16f;
		float minV = far ? 0 : animation;
		float maxV = far ? 1 / 16f : length + minV;
		float minU = far ? 3 / 16f : 0;
		float maxU = far ? 4 / 16f : 3 / 16f;

		PoseStack ms = new PoseStack();
		ms.last()
			.set(pose);
		ms.translate(0.5D, 0.0D, 0.5D);

		renderPart(ms, vc, length, 0.0F, radius, radius, 0.0F, -radius, 0.0F, 0.0F, -radius, minU, maxU, minV, maxV,
			light1, light2, far);
	}

	private static void renderPart(PoseStack pPoseStack, VertexConsumer pConsumer, float pMaxY, float pX0, float pZ0,
		float pX1, float pZ1, float pX2, float pZ2, float pX3, float pZ3, float pMinU, float pMaxU, float pMinV,
		float pMaxV, int light1, int light2, boolean far) {
		PoseStack.Pose posestack$pose = pPoseStack.last();
		Matrix4f matrix4f = posestack$pose.pose();

		float uO = far ? 0f : 3 / 16f;
		renderQuad(matrix4f, posestack$pose, pConsumer, 0, pMaxY, pX0, pZ0, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV, light1,
			light2);
		renderQuad(matrix4f, posestack$pose, pConsumer, 0, pMaxY, pX3, pZ3, pX0, pZ0, pMinU, pMaxU, pMinV, pMaxV, light1,
			light2);
		renderQuad(matrix4f, posestack$pose, pConsumer, 0, pMaxY, pX1, pZ1, pX2, pZ2, pMinU + uO, pMaxU + uO, pMinV, pMaxV,
			light1, light2);
		renderQuad(matrix4f, posestack$pose, pConsumer, 0, pMaxY, pX2, pZ2, pX1, pZ1, pMinU + uO, pMaxU + uO, pMinV, pMaxV,
			light1, light2);
	}

	private static void renderQuad(Matrix4f pPose, PoseStack.Pose pNormal, VertexConsumer pConsumer, float pMinY, float pMaxY,
		float pMinX, float pMinZ, float pMaxX, float pMaxZ, float pMinU, float pMaxU, float pMinV, float pMaxV,
		int light1, int light2) {
		addVertex(pPose, pNormal, pConsumer, pMaxY, pMinX, pMinZ, pMaxU, pMinV, light2);
		addVertex(pPose, pNormal, pConsumer, pMinY, pMinX, pMinZ, pMaxU, pMaxV, light1);
		addVertex(pPose, pNormal, pConsumer, pMinY, pMaxX, pMaxZ, pMinU, pMaxV, light1);
		addVertex(pPose, pNormal, pConsumer, pMaxY, pMaxX, pMaxZ, pMinU, pMinV, light2);
	}

	private static void addVertex(Matrix4f pPose, PoseStack.Pose pNormal, VertexConsumer pConsumer, float pY, float pX,
		float pZ, float pU, float pV, int light) {
		pConsumer.addVertex(pPose, pX, pY, pZ)
			.setColor(1.0f, 1.0f, 1.0f, 1.0f)
			.setUv(pU, pV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pNormal, 0.0F, 1.0F, 0.0F);
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected SuperByteBuffer getRotatedModel(ChainConveyorBlockEntity be, BlockState state) {
		return CachedBufferer.partial(AllPartialModels.CHAIN_CONVEYOR_SHAFT, state);
	}

	@Override
	protected RenderType getRenderType(ChainConveyorBlockEntity be, BlockState state) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock();
	}

}
