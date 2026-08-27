package com.simibubi.create;

import net.minecraft.tags.BlockItemTags;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.LeverMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

public class AllInteractionBehaviours {
	static void registerDefaults() {
		MovingInteractionBehaviour.REGISTRY.register(Blocks.LEVER, new LeverMovingInteraction());

		MovingInteractionBehaviour.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(BlockItemTags.WOODEN_DOORS.block(), new DoorMovingInteraction()));
		MovingInteractionBehaviour.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(BlockItemTags.WOODEN_TRAPDOORS.block(), new TrapdoorMovingInteraction()));
		MovingInteractionBehaviour.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(BlockItemTags.FENCE_GATES.block(), new TrapdoorMovingInteraction()));
	}
}
