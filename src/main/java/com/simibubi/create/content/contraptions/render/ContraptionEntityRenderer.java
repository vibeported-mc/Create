package com.simibubi.create.content.contraptions.render;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.createmod.catnip.api.client.render.SuperByteBufferRenderState;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ClientContraption.RenderedBlocks;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.ShadedBlockSbbBuilder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;


public class ContraptionEntityRenderer<C extends AbstractContraptionEntity, S extends ContraptionEntityRenderer.ContraptionRenderState>
	extends EntityRenderer<C, S> {
	public static final SuperByteBufferCache.Compartment<Contraption> CONTRAPTION = new SuperByteBufferCache.Compartment<>();

	/**
	 * A contraption is drawn from its own virtual level, which the submit phase may not touch, so
	 * the structure geometry and everything its block entities and actors need is resolved during
	 * extraction and replayed from here.
	 */
	public static class ContraptionRenderState extends EntityRenderState {
		/** Set during extraction when there is nothing to draw; submission still runs regardless. */
		public boolean skip;
		public @Nullable SuperByteBufferRenderState structure;
		public final List<ActorGeometry> actors = new ArrayList<>();
		public BlockEntityRenderHelper.@Nullable Extracted blockEntities;
	}


	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	public ContraptionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public static SuperByteBuffer getBuffer(Contraption contraption, VirtualRenderWorld renderWorld) {
		return SuperByteBufferCache.getInstance()
			.get(CONTRAPTION, contraption, () -> buildStructureBuffer(contraption, renderWorld));
	}

	private static SuperByteBuffer buildStructureBuffer(Contraption contraption, VirtualRenderWorld renderWorld) {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
		RandomSource random = objects.random;
		var clientContraption = contraption.getOrCreateClientContraptionLazy();
		RenderedBlocks blocks = clientContraption.getRenderedBlocks();

		ShadedBlockSbbBuilder sbbBuilder = objects.sbbBuilder;
		sbbBuilder.begin();

		Minecraft mc = Minecraft.getInstance();
		BlockStateModelSet models = mc.getModelManager()
			.getBlockStateModelSet();
		ModelBlockRenderer renderer = new ModelBlockRenderer(mc.options.ambientOcclusion()
			.get(), false, mc.getBlockColors());

		BlockModelLighter.enableCaching();
		for (BlockPos pos : blocks.positions()) {
			BlockState state = blocks.lookup()
				.apply(pos);
			if (state.getRenderShape() != RenderShape.MODEL)
				continue;
			long randomSeed = state.getSeed(pos);
			random.setSeed(randomSeed);
			renderer.tesselateBlock(sbbBuilder::putBlockBakedQuad, pos.getX(), pos.getY(), pos.getZ(), renderWorld, pos,
				state, models.get(state), randomSeed);
		}
		BlockModelLighter.clearCache();

		return sbbBuilder.end();
	}

	@Override
	public boolean shouldRender(C entity, Frustum frustum, double cameraX, double cameraY,
		double cameraZ) {
		if (entity.getContraption() == null)
			return false;
		if (!entity.isAliveOrStale())
			return false;
		if (!entity.isReadyForRender())
			return false;

		return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
	}

	@Override
	@SuppressWarnings("unchecked")
	public S createRenderState() {
		return (S) new ContraptionRenderState();
	}

	@Override
	public void extractRenderState(C entity, S state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.structure = null;
		state.actors.clear();

		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		Level level = entity.level();
		ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
		VirtualRenderWorld renderWorld = clientContraption.getRenderLevel();
		ContraptionMatrices matrices = clientContraption.getMatrices();
		matrices.setup(new PoseStack(), entity);

		if (!VisualizationManager.supportsVisualization(level)) {
			SuperByteBuffer sbb = getBuffer(contraption, renderWorld);
			if (!sbb.isEmpty())
				state.structure = sbb.transform(matrices.getModel())
					.useLevelLight(renderWorld, matrices.getWorld())
					.extractRenderState();
		}

		var adjustRenderedBlockEntities = clientContraption.getAndAdjustShouldRenderBlockEntities();
		clientContraption.scratchErroredBlockEntities.clear();
		state.blockEntities = BlockEntityRenderHelper.extractBlockEntities(clientContraption.renderedBlockEntityView,
			adjustRenderedBlockEntities, clientContraption.scratchErroredBlockEntities, renderWorld, level,
			matrices.getLight(), entity.toLocalVector(entityRenderDispatcher.camera.position(), partialTicks),
			partialTicks);
		clientContraption.shouldRenderBlockEntities.andNot(clientContraption.scratchErroredBlockEntities);

		extractActors(level, renderWorld, contraption, matrices, state.actors);
		matrices.clear();
	}

	@Override
	public void submit(S state, PoseStack poseStack, SubmitNodeCollector queue, CameraRenderState camera) {
		super.submit(state, poseStack, queue, camera);
		if (state.skip)
			return;

		if (state.structure != null)
			state.structure.submit(poseStack, RenderTypes.solidMovingBlock(), queue);
		if (state.blockEntities != null)
			state.blockEntities.submit(poseStack, queue, camera);
		for (ActorGeometry actor : state.actors)
			actor.submit(poseStack, queue);
	}

	private static void extractActors(Level level, VirtualRenderWorld renderWorld, Contraption c,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		PoseStack m = matrices.getModel();

		for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : c.getActors()) {
			MovementContext context = actor.getRight();
			if (context == null)
				continue;
			if (context.world == null)
				context.world = level;
			StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();

			MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(blockInfo.state());
			if (movementBehaviour != null) {
				if (c.isHiddenInPortal(blockInfo.pos()))
					continue;
				m.pushPose();
				TransformStack.of(m)
					.translate(blockInfo.pos());
				movementBehaviour.extractInContraption(context, renderWorld, matrices, out);
				m.popPose();
			}
		}
	}

	private static class ThreadLocalObjects {
		public final PoseStack poseStack = new PoseStack();
		public final RandomSource random = RandomSource.createThreadLocalInstance();
		public final ShadedBlockSbbBuilder sbbBuilder = ShadedBlockSbbBuilder.create();
	}
}
