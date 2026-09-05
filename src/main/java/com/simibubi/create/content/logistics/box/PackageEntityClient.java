package com.simibubi.create.content.logistics.box;

import net.createmod.ponder.api.client.level.PonderLevel;

/**
 * The half of a package's tick that only means anything in a Ponder scene.
 *
 * Here rather than in {@link PackageEntity} because {@code PonderLevel} is a client class, and the
 * dedicated server has no such class to load. Naming it in the entity's own {@code tick} was enough
 * to bring a server down: the first package to tick reached the {@code instanceof}, the loader was
 * asked for a class that is not on that side, and what came back was
 * {@code ClassNotFoundException: ... which is not present on the dedicated server} inside
 * {@code Ticking entity} -- a crash whose report names a rendering class and says nothing about
 * packages.
 *
 * @see com.simibubi.create.content.trains.track.TrackTargetingClient for the same split, made for the
 *      same reason
 */
public class PackageEntityClient {

	/**
	 * Ponder has no physics of its own, so a package falls because this makes it fall.
	 *
	 * Called only from the client side, and only ever true in a Ponder scene: everywhere else a
	 * package is moved by whatever is carrying it.
	 */
	public static void tickInPonder(PackageEntity entity) {
		if (!(entity.level() instanceof PonderLevel))
			return;

		entity.setDeltaMovement(entity.getDeltaMovement()
			.add(0, -0.06, 0));
		if (entity.position().y < 0.125)
			entity.discard();
	}

}
