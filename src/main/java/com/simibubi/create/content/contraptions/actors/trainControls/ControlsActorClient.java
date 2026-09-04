package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

/**
 * How this actor draws itself, kept out of its behaviour so a dedicated server never loads it.
 *
 * @see com.simibubi.create.api.behaviour.movement.MovementBehaviourClient
 */
public class ControlsActorClient implements MovementBehaviourClient {

	@Override
	public void extractInContraption(MovementBehaviour behaviour, MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		if (!(context.temporaryData instanceof ControlsMovementBehaviour.LeverAngles angles))
			return;

		AbstractContraptionEntity entity = context.contraption.entity;
		if (!(entity instanceof CarriageContraptionEntity cce))
			return;

		StructureBlockInfo info = context.contraption.getBlocks()
			.get(context.localPos);
		Direction initialOrientation = cce.getInitialOrientation()
			.getCounterClockWise();
		boolean inverted = false;
		if (info != null && info.state().hasProperty(ControlsBlock.FACING))
			inverted = !info.state().getValue(ControlsBlock.FACING)
				.equals(initialOrientation);

		if (ControlsHandler.getContraption() == entity && ControlsHandler.getControlsPos() != null
			&& ControlsHandler.getControlsPos().equals(context.localPos)) {
			Collection<Integer> pressed = ControlsHandler.currentlyPressed;
			angles.equipAnimation.chase(1, .2f, Chaser.EXP);
			angles.steering.chase((pressed.contains(3) ? 1 : 0) + (pressed.contains(2) ? -1 : 0), 0.2f, Chaser.EXP);
			float f = cce.movingBackwards ^ inverted ? -1 : 1;
			angles.speed.chase(Math.min(context.motion.length(), 0.5f) * f, 0.2f, Chaser.EXP);

		} else {
			angles.equipAnimation.chase(0, .2f, Chaser.EXP);
			angles.steering.chase(0, 0, Chaser.EXP);
			angles.speed.chase(0, 0, Chaser.EXP);
		}

		float pt = AnimationTickHolder.getPartialTicks(context.world);
		ControlsRenderer.extract(context, renderWorld, matrices, out, angles.equipAnimation.getValue(pt),
			angles.speed.getValue(pt), angles.steering.getValue(pt));
	}

}
