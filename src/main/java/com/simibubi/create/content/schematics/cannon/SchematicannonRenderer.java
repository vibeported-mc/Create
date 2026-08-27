package com.simibubi.create.content.schematics.cannon;

import com.simibubi.create.foundation.render.CachedBufferer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForBelt;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForBlockState;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.impl.neoforge.render.VirtualRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SchematicannonRenderer
	extends SafeBlockEntityRenderer<SchematicannonBlockEntity, SchematicannonRenderer.SchematicannonRenderState> {

	private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	public static class SchematicannonRenderState extends SafeRenderState {
		public @Nullable SuperByteBufferRenderState connector;
		public @Nullable SuperByteBufferRenderState pipe;
		public final List<LaunchedRenderState> launched = new ArrayList<>();
	}

	protected final BlockModelResolver blockModelResolver;
	protected final ItemModelResolver itemModelResolver;

	public SchematicannonRenderer(BlockEntityRendererProvider.Context context) {
		blockModelResolver = context.blockModelResolver();
		itemModelResolver = context.itemModelResolver();
	}

	@Override
	public SchematicannonRenderState createRenderState() {
		return new SchematicannonRenderState();
	}

	@Override
	protected void extractSafe(SchematicannonBlockEntity be, SchematicannonRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.launched.clear();
		state.connector = null;
		state.pipe = null;

		if (!be.flyingBlocks.isEmpty())
			extractLaunchedBlocks(be, state, partialTicks);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockPos pos = be.getBlockPos();
		BlockState blockState = be.getBlockState();

		double[] cannonAngles = getCannonAngles(be, pos, partialTicks);
		double yaw = cannonAngles[0];
		double pitch = cannonAngles[1];
		double recoil = getRecoil(be, partialTicks);

		SuperByteBuffer connector = CachedBufferer.partial(AllPartialModels.SCHEMATICANNON_CONNECTOR, blockState);
		TransformStack.of(connector.getTransforms())
			.translate(.5f, 0, .5f)
			.rotate((float) ((yaw + 90) / 180 * Math.PI), Direction.UP)
			.translate(-.5f, 0, -.5f);
		state.connector = connector.light(state.lightCoords)
			.extractRenderState();

		SuperByteBuffer pipe = CachedBufferer.partial(AllPartialModels.SCHEMATICANNON_PIPE, blockState);
		TransformStack.of(pipe.getTransforms())
			.translate(.5f, 15 / 16f, .5f)
			.rotate((float) ((yaw + 90) / 180 * Math.PI), Direction.UP)
			.rotate((float) (pitch / 180 * Math.PI), Direction.SOUTH)
			.translate(-.5f, -15 / 16f, -.5f)
			.translate(0, -recoil / 100, 0);
		state.pipe = pipe.light(state.lightCoords)
			.extractRenderState();
	}

	@Override
	protected void submitSafe(SchematicannonRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (LaunchedRenderState launched : state.launched)
			launched.submit(ms, queue, state.lightCoords);

		if (state.connector != null)
			state.connector.submit(ms, RenderTypes.solidMovingBlock(), queue);
		if (state.pipe != null)
			state.pipe.submit(ms, RenderTypes.solidMovingBlock(), queue);
	}

	public static double[] getCannonAngles(SchematicannonBlockEntity blockEntity, BlockPos pos, float partialTicks) {
		double yaw;
		double pitch;

		BlockPos target = blockEntity.printer.getCurrentTarget();
		if (target != null) {

			// Calculate Angle of Cannon
			Vec3 diff = Vec3.atLowerCornerOf(target.subtract(pos));
			if (blockEntity.previousTarget != null) {
				diff = (Vec3.atLowerCornerOf(blockEntity.previousTarget)
					.add(Vec3.atLowerCornerOf(target.subtract(blockEntity.previousTarget))
						.scale(partialTicks))).subtract(Vec3.atLowerCornerOf(pos));
			}

			double diffX = diff.x();
			double diffZ = diff.z();
			yaw = Mth.atan2(diffX, diffZ);
			yaw = yaw / Math.PI * 180;

			float distance = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
			double yOffset = 0 + distance * 2f;
			pitch = Mth.atan2(distance, diff.y() * 3 + yOffset);
			pitch = pitch / Math.PI * 180 + 10;

		} else {
			yaw = blockEntity.defaultYaw;
			pitch = 40;
		}

		return new double[] { yaw, pitch };
	}

	public static double getRecoil(SchematicannonBlockEntity blockEntity, float partialTicks) {
		double recoil = 0;

		for (LaunchedItem launched : blockEntity.flyingBlocks) {

			if (launched.ticksRemaining == 0)
				continue;

			// Apply Recoil if block was just launched
			if ((launched.ticksRemaining + 1 - partialTicks) > launched.totalTicks - 10)
				recoil = Math.max(recoil, (launched.ticksRemaining + 1 - partialTicks) - launched.totalTicks + 10);
		}

		return recoil;
	}

	/**
	 * Blocks and items in flight are resolved into render states here; the flight path is a function of
	 * the tick, so its position is baked in too. The launch particles are spawned during extraction
	 * because they mutate the block entity, which submission must not touch.
	 */
	private void extractLaunchedBlocks(SchematicannonBlockEntity be, SchematicannonRenderState state,
		float partialTicks) {
		for (LaunchedItem launched : be.flyingBlocks) {

			if (launched.ticksRemaining == 0)
				continue;

			// Calculate position of flying block
			Vec3 start = Vec3.atCenterOf(be.getBlockPos()
				.above());
			Vec3 target = Vec3.atCenterOf(launched.target);
			Vec3 distance = target.subtract(start);

			double yDifference = target.y - start.y;
			double throwHeight = Math.sqrt(distance.lengthSqr()) * .6f + yDifference;
			Vec3 cannonOffset = distance.add(0, throwHeight, 0)
				.normalize()
				.scale(2);
			start = start.add(cannonOffset);
			yDifference = target.y - start.y;

			float progress =
				((float) launched.totalTicks - (launched.ticksRemaining + 1 - partialTicks)) / launched.totalTicks;
			Vec3 blockLocationXZ = target.subtract(start)
				.scale(progress)
				.multiply(1, 0, 1);

			// Height is determined through a bezier curve
			float t = progress;
			double yOffset = 2 * (1 - t) * t * throwHeight + t * t * yDifference;
			Vec3 blockLocation = blockLocationXZ.add(0.5, yOffset + 1.5, 0.5)
				.add(cannonOffset);

			if (launched instanceof ForBlockState) {
				BlockState blockState;
				if (launched instanceof ForBelt) {
					// Render a shaft instead of the belt
					blockState = AllBlocks.SHAFT.getDefaultState();
				} else {
					blockState = ((ForBlockState) launched).state;
				}
				BlockModelRenderState model = new BlockModelRenderState();
				blockModelResolver.update(model, blockState, BLOCK_DISPLAY_CONTEXT);
				state.launched.add(new LaunchedRenderState(blockLocation, t, model, null));

			} else if (launched instanceof ForEntity) {
				ItemStackRenderState item = new ItemStackRenderState();
				item.displayContext = ItemDisplayContext.GROUND;
				itemModelResolver.appendItemLayers(item, launched.stack, ItemDisplayContext.GROUND, be.getLevel(),
					null, 0);
				state.launched.add(new LaunchedRenderState(blockLocation, t, null, item));
			}

			// Render particles for launch
			if (launched.ticksRemaining == launched.totalTicks && be.firstRenderTick) {
				start = start.subtract(.5, .5, .5);
				be.firstRenderTick = false;
				for (int i = 0; i < 10; i++) {
					RandomSource r = be.getLevel()
						.getRandom();
					double sX = cannonOffset.x * .01f;
					double sY = (cannonOffset.y + 1) * .01f;
					double sZ = cannonOffset.z * .01f;
					double rX = r.nextFloat() - sX * 40;
					double rY = r.nextFloat() - sY * 40;
					double rZ = r.nextFloat() - sZ * 40;
					be.getLevel()
						.addParticle(ParticleTypes.CLOUD, start.x + rX, start.y + rY, start.z + rZ, sX, sY, sZ);
				}
			}

		}
	}

	public record LaunchedRenderState(Vec3 location, float spin, @Nullable BlockModelRenderState block,
		@Nullable ItemStackRenderState item) {

		public void submit(PoseStack ms, SubmitNodeCollector queue, int light) {
			ms.pushPose();
			ms.translate(location.x, location.y, location.z);

			ms.translate(.125f, .125f, .125f);
			ms.mulPose(Axis.YP.rotationDegrees(360 * spin));
			ms.mulPose(Axis.XP.rotationDegrees(360 * spin));
			ms.translate(-.125f, -.125f, -.125f);

			if (block != null) {
				float scale = .3f;
				ms.scale(scale, scale, scale);
				block.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
			} else if (item != null) {
				float scale = 1.2f;
				ms.scale(scale, scale, scale);
				item.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0);
			}

			ms.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

}
