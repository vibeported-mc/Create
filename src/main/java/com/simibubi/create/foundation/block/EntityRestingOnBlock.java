package com.simibubi.create.foundation.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * A block that is told about whatever comes to rest on it, for as long as it rests there.
 * <p>
 * The game used to ask this of every block an entity settled onto, through
 * {@code Block.updateEntityAfterFallOn}, on every move the entity made - so a block being landed on
 * heard about it again and again while the thing stayed put. 26.2 dropped that method; what is left,
 * {@code fallOn}, is called once, on the way down.
 * <p>
 * The difference matters to anything that might decline the first time and want asking again: a chute
 * whose filter will only take flint refuses the gravel that lands on it, and a fan then washes that
 * gravel where it lies. Asked once, the chute never learns the answer changed. So Create asks for
 * itself, from the same place in a move the game used to.
 */
public interface EntityRestingOnBlock {

	void updateEntityAfterFallOn(Level level, Entity entity);

}
