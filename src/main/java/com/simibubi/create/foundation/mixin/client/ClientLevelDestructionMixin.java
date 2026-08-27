package com.simibubi.create.foundation.mixin.client;

import java.util.Set;
import java.util.SortedSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Sets;
import com.simibubi.create.foundation.block.render.BlockDestructionProgressExtension;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

/**
 * Lets a block that occupies more than one position crack all of them at once.
 * <p>
 * 26.2 moved block destruction progress off the level renderer and onto the client level itself, so
 * that is what this mixes into now. The progress entry is looked up by breaker rather than captured
 * as a local, which does not depend on the shape of the method around the injection point.
 */
@Mixin(ClientLevel.class)
public class ClientLevelDestructionMixin {

	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Shadow
	@Final
	private Int2ObjectMap<BlockDestructionProgress> destroyingBlocks;

	@Inject(method = "destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V", at = @At("TAIL"))
	private void create$onDestroyBlockProgress(int breakerId, BlockPos pos, int progress, CallbackInfo ci) {
		BlockDestructionProgress progressObj = destroyingBlocks.get(breakerId);
		if (progressObj == null)
			return;

		ClientLevel level = (ClientLevel) (Object) this;
		BlockState state = level.getBlockState(pos);
		IClientBlockExtensions properties = IClientBlockExtensions.of(state);
		if (!(properties instanceof MultiPosDestructionHandler handler))
			return;

		Set<BlockPos> extraPositions = handler.getExtraPositions(level, pos, state, progress);
		if (extraPositions == null)
			return;

		extraPositions.remove(pos);
		((BlockDestructionProgressExtension) progressObj).create$setExtraPositions(extraPositions);
		for (BlockPos extraPos : extraPositions)
			destructionProgress.computeIfAbsent(extraPos.asLong(), l -> Sets.newTreeSet())
				.add(progressObj);
	}

	@Inject(method = "removeProgress(Lnet/minecraft/server/level/BlockDestructionProgress;)V", at = @At("RETURN"))
	private void create$onRemoveProgress(BlockDestructionProgress progress, CallbackInfo ci) {
		Set<BlockPos> extraPositions = ((BlockDestructionProgressExtension) progress).create$getExtraPositions();
		if (extraPositions == null)
			return;

		for (BlockPos extraPos : extraPositions) {
			long l = extraPos.asLong();
			Set<BlockDestructionProgress> set = destructionProgress.get(l);
			if (set != null) {
				set.remove(progress);
				if (set.isEmpty())
					destructionProgress.remove(l);
			}
		}
	}
}
