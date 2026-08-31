package com.simibubi.create.foundation.render;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperBufferFactory;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.createmod.catnip.api.client.render.SuperByteBufferCache.Compartment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cached buffers for Flywheel's partial models.
 * <p>
 * These used to live alongside {@link CachedBuffers} in Catnip, but Catnip dropped its Flywheel
 * dependency and {@link PartialModel} is a Flywheel type, so they belong here now. The block-state
 * buffers stay in Catnip; only the partial-model ones moved.
 */
public class CachedBufferer {

	public static final Compartment<PartialModel> PARTIAL = new Compartment<>();
	public static final Compartment<Pair<Direction, PartialModel>> DIRECTIONAL_PARTIAL = new Compartment<>();

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState) {
		return SuperByteBufferCache.getInstance()
			.get(PARTIAL, partial, () -> SuperBufferFactory.getInstance()
				.createForBlock(partial.get(), referenceState));
	}

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState,
		Supplier<PoseStack> modelTransform) {
		return SuperByteBufferCache.getInstance()
			.get(PARTIAL, partial, () -> SuperBufferFactory.getInstance()
				.createForBlock(partial.get(), referenceState, modelTransform.get()));
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState) {
		return partialFacing(partial, referenceState, referenceState.getValue(FACING));
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState, Direction facing) {
		return partialDirectional(partial, referenceState, facing, CachedBuffers.rotateToFace(facing));
	}

	public static SuperByteBuffer partialFacingVertical(PartialModel partial, BlockState referenceState,
		Direction facing) {
		return partialDirectional(partial, referenceState, facing, CachedBuffers.rotateToFaceVertical(facing));
	}

	public static SuperByteBuffer partialDirectional(PartialModel partial, BlockState referenceState, Direction dir,
		Supplier<PoseStack> modelTransform) {
		return SuperByteBufferCache.getInstance()
			.get(DIRECTIONAL_PARTIAL, Pair.of(dir, partial), () -> SuperBufferFactory.getInstance()
				.createForBlock(partial.get(), referenceState, modelTransform.get()));
	}

	private CachedBufferer() {
	}

}
