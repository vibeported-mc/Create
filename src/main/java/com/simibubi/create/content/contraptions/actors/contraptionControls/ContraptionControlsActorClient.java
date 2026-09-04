package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.RegistryNbt;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import java.util.List;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.IntAttached;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * How this actor draws itself, kept out of its behaviour so a dedicated server never loads it.
 *
 * @see com.simibubi.create.api.behaviour.movement.MovementBehaviourClient
 */
public class ContraptionControlsActorClient implements MovementBehaviourClient {

	@Override
	public void extractInContraption(MovementBehaviour behaviour, MovementContext ctx, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, List<ActorGeometry> out) {
		ContraptionControlsRenderer.extractInContraption(ctx, renderWorld, matrices, out);
	}

}
