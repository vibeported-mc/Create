package com.simibubi.create.content.schematics.client;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;

import net.createmod.catnip.api.client.level.wrapper.WrappedClientLevel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.level.wrapper.SchematicLevel;
import net.createmod.catnip.api.client.render.ShadedBlockSbbBuilder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;


public class SchematicRenderer {

	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	private @Nullable SuperByteBuffer buffer;
	private boolean changed;
	protected final SchematicLevel schematic;
	private final BlockPos anchor;
	private final List<BlockEntity> renderedBlockEntities = new ArrayList<>();
	private final BitSet shouldRenderBlockEntities = new BitSet();
	private final BitSet scratchErroredBlockEntities = new BitSet();

	public SchematicRenderer(SchematicLevel world) {
		this.anchor = world.anchor;
		this.schematic = world;
		this.changed = true;

		for (var renderedBlockEntity : schematic.getRenderedBlockEntities()) {
			renderedBlockEntities.add(renderedBlockEntity);
		}
		shouldRenderBlockEntities.set(0, renderedBlockEntities.size());
	}

	public void update() {
		changed = true;
	}

	public void submit(PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		if (changed)
			redraw();
		changed = false;

		if (buffer != null && !buffer.isEmpty())
			buffer.submit(ms, RenderTypes.solidMovingBlock(), queue);

		scratchErroredBlockEntities.clear();
		float pt = AnimationTickHolder.getPartialTicks();
		// The schematic is not in the level's render pass, so its block entities are extracted and
		// submitted together here.
		BlockEntityRenderHelper
			.extractBlockEntities(renderedBlockEntities, shouldRenderBlockEntities, scratchErroredBlockEntities, null,
				schematic, null, camera.pos, pt)
			.submit(ms, queue, camera);

		// Don't bother looping over errored BEs again.
		shouldRenderBlockEntities.andNot(scratchErroredBlockEntities);
	}

	protected void redraw() {
		buffer = drawSchematic();
	}

	protected SuperByteBuffer drawSchematic() {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();

		RandomSource random = objects.random;
		BlockPos.MutableBlockPos mutableBlockPos = objects.mutableBlockPos;
		SchematicLevel renderWorld = schematic;
		BoundingBox bounds = renderWorld.getBounds();

		ShadedBlockSbbBuilder sbbBuilder = objects.sbbBuilder;
		sbbBuilder.begin();

		Minecraft mc = Minecraft.getInstance();
		BlockStateModelSet models = mc.getModelManager()
			.getBlockStateModelSet();
		ModelBlockRenderer renderer = new ModelBlockRenderer(mc.options.ambientOcclusion()
			.get(), false, mc.getBlockColors());

		renderWorld.renderMode = true;
		BlockModelLighter.enableCaching();
		for (BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
			BlockPos pos = mutableBlockPos.setWithOffset(localPos, anchor);
			BlockState state = renderWorld.getBlockState(pos);

			if (state.getRenderShape() != RenderShape.MODEL)
				continue;

			long seed = state.getSeed(pos);
			random.setSeed(seed);
			renderer.tesselateBlock(sbbBuilder::putBlockBakedQuad, localPos.getX(), localPos.getY(), localPos.getZ(),
				WrappedClientLevel.of(renderWorld), pos, state, models.get(state), seed);
		}
		BlockModelLighter.clearCache();
		renderWorld.renderMode = false;

		return sbbBuilder.end();
	}

	private static class ThreadLocalObjects {
		public final RandomSource random = RandomSource.createThreadLocalInstance();
		public final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		public final ShadedBlockSbbBuilder sbbBuilder = ShadedBlockSbbBuilder.create();
	}

}
