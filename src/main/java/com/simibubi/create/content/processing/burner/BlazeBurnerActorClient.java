package com.simibubi.create.content.processing.burner;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import java.util.List;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How this actor draws itself, kept out of its behaviour so a dedicated server never loads it.
 *
 * @see com.simibubi.create.api.behaviour.movement.MovementBehaviourClient
 */
public class BlazeBurnerActorClient implements MovementBehaviourClient {

	@Override
	public void extractInContraption(MovementBehaviour behaviour, MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, List<ActorGeometry> out) {
		if (!(behaviour instanceof BlazeBurnerMovementBehaviour burner) || !burner.shouldRender(context))
			return;
		BlazeBurnerRenderer.extractInContraption(context, renderWorld, matrices, out, burner.getHeadAngle(context),
			burner.shouldRenderHat(context));
	}

}
