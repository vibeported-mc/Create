package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How this actor draws itself, kept out of its behaviour so a dedicated server never loads it.
 *
 * @see com.simibubi.create.api.behaviour.movement.MovementBehaviourClient
 */
public class PortableStorageInterfaceActorClient implements MovementBehaviourClient {

	@Override
	@Nullable
	public ActorVisual createVisual(MovementBehaviour behaviour, VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
		MovementContext movementContext) {
		return new PSIActorVisual(visualizationContext, simulationWorld, movementContext);
	}

	@Override
	public void extractInContraption(MovementBehaviour behaviour, MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		if (!VisualizationManager.supportsVisualization(context.world))
			PortableStorageInterfaceRenderer.extractInContraption(context, renderWorld, matrices, out);
	}

}
