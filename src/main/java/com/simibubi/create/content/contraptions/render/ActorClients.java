package com.simibubi.create.content.contraptions.render;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviourClient;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsActorClient;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterActorClient;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceActorClient;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.actors.roller.RollerActorClient;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsActorClient;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingActorClient;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerActorClient;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.kinetics.drill.DrillActorClient;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.SawActorClient;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerActorClient;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;

/**
 * Which client-side renderer belongs to which actor.
 * <p>
 * The table lives here rather than on the behaviours because a behaviour must not so much as name a
 * rendering type: it is instantiated during registration, on a dedicated server, where those types
 * do not exist. Splitting {@link MovementBehaviourClient} out was not enough on its own -- a
 * behaviour that merely implemented it still failed to link, since building its interface method
 * table resolves the descriptors just the same.
 * <p>
 * Keyed by the behaviour's class, so a subclass that adds no rendering of its own inherits nothing
 * and simply draws nothing. An addon wanting to draw an actor registers here.
 */
public final class ActorClients {

	private static final Map<Class<? extends MovementBehaviour>, MovementBehaviourClient> BY_BEHAVIOUR =
		new IdentityHashMap<>();

	static {
		register(ContraptionControlsMovement.class, new ContraptionControlsActorClient());
		register(HarvesterMovementBehaviour.class, new HarvesterActorClient());
		register(PortableStorageInterfaceMovement.class, new PortableStorageInterfaceActorClient());
		register(RollerMovementBehaviour.class, new RollerActorClient());
		register(ControlsMovementBehaviour.class, new ControlsActorClient());
		register(StabilizedBearingMovementBehaviour.class, new StabilizedBearingActorClient());
		register(DeployerMovementBehaviour.class, new DeployerActorClient());
		register(DrillMovementBehaviour.class, new DrillActorClient());
		register(SawMovementBehaviour.class, new SawActorClient());
		register(BlazeBurnerMovementBehaviour.class, new BlazeBurnerActorClient());
	}

	public static void register(Class<? extends MovementBehaviour> behaviour, MovementBehaviourClient client) {
		BY_BEHAVIOUR.put(behaviour, client);
	}

	/** What draws this actor, or null if nothing does. */
	@Nullable
	public static MovementBehaviourClient of(MovementBehaviour behaviour) {
		return BY_BEHAVIOUR.get(behaviour.getClass());
	}

	private ActorClients() {}
}
