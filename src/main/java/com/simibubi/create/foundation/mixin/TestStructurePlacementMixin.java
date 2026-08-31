package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;

/**
 * Lay a game test's structure the way structures used to be laid.
 * <p>
 * A block written down in one world and put down in another brings that world's coordinates with it -
 * a belt naming the head of its run, a kinetic block naming what turns it - and the block is left
 * pointing at somewhere that has nothing to do with where it now is. Placing has always sorted this
 * out: it tells every block its shape may have changed, and Create takes that as the moment to forget
 * what it remembered and work itself out from its actual surroundings.
 * <p>
 * Test structures are now laid claiming the shape is already known, which is exactly the flag that
 * skips telling them, so they wake up believing what was written down. A gearbox reverses rotation it
 * was never given and the propagator pulls the generator apart over the disagreement; a belt turns and
 * carries nothing. Both were being answered a block at a time, in the blocks themselves - this says it
 * once, where the claim is made.
 */
@Mixin(TestInstanceBlockEntity.class)
public class TestStructurePlacementMixin {

	@ModifyArg(method = "placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;setKnownShape(Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"), index = 0)
	private boolean create$dontClaimTheShapeIsKnown(boolean knownShape) {
		return false;
	}

	@ModifyArg(method = "placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z"), index = 5)
	private int create$letTheBlocksHearTheirShapeChanged(int flags) {
		return flags & ~Block.UPDATE_KNOWN_SHAPE;
	}
}
