package com.simibubi.create.api.behaviour.movement;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorGeometry;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;

/**
 * How an actor draws itself.
 *
 * <h2>Why this is not part of {@link MovementBehaviour}</h2>
 *
 * Both methods name {@link VirtualRenderWorld}, which implements
 * {@code net.minecraft.client.renderer.block.BlockAndTintGetter} - a type 26.2 moved out of
 * {@code net.minecraft.world.level} and into the client. The JVM resolves a method's parameter types
 * when it links a class against it, so a behaviour declaring these could not be loaded on a
 * dedicated server. Actors are instantiated during registration, so that took the whole mod down.
 * <p>
 * Splitting the interface is necessary but not sufficient, and that part is worth writing down
 * because it was measured rather than guessed: a behaviour that merely {@code implements} this
 * interface still fails, because building its interface method table resolves these descriptors
 * just the same. So a behaviour must not implement it at all. The client keeps its own table
 * instead, in {@code ActorClients}, and a behaviour on a server never mentions rendering.
 * <p>
 * Until 26.2 the two methods carried {@code @OnlyIn(Dist.CLIENT)} and were stripped from the server
 * jar, which is why none of this arose before. NeoForge no longer strips annotated members.
 *
 * @see MovementBehaviour
 */
public interface MovementBehaviourClient {

	/**
	 * Collect this actor's geometry. 26.2 runs this on the client thread, ahead of submission, so
	 * this is the only place the contraption and its virtual level may be read.
	 */
	default void extractInContraption(MovementBehaviour behaviour, MovementContext context,
		VirtualRenderWorld renderWorld, ContraptionMatrices matrices, List<ActorGeometry> out) {}

	@Nullable
	default ActorVisual createVisual(MovementBehaviour behaviour, VisualizationContext visualizationContext,
		VirtualRenderWorld simulationWorld, MovementContext movementContext) {
		return null;
	}
}
