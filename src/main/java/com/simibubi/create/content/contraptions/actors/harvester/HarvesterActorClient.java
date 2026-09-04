package com.simibubi.create.content.contraptions.actors.harvester;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.CropBlockAccessor;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.List;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SpecialPlantable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

/**
 * How this actor draws itself, kept out of its behaviour so a dedicated server never loads it.
 *
 * @see com.simibubi.create.api.behaviour.movement.MovementBehaviourClient
 */
public class HarvesterActorClient implements MovementBehaviourClient {

	@Override
	public void extractInContraption(MovementBehaviour behaviour, MovementContext context, VirtualRenderWorld renderWorld,
									ContraptionMatrices matrices, List<ActorGeometry> out) {
		if (!VisualizationManager.supportsVisualization(context.world))
			HarvesterRenderer.extractInContraption(context, renderWorld, matrices, out);
	}

	@Override
	@Nullable
	public ActorVisual createVisual(MovementBehaviour behaviour, VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
									MovementContext movementContext) {
		return new HarvesterActorVisual(visualizationContext, simulationWorld, movementContext);
	}

}
