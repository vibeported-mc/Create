package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Water carried from one tank to another by a contraption that only turns on the spot.
 * <p>
 * A bearing turns a tank, a casing and a portable fluid interface, all glued into one thing. The
 * interface swings round to a fixed one beside the full tank, a pump fills the travelling tank through
 * it, and half a turn later a second pump empties it into the far tank. Nothing joins the two ends but
 * the contraption going round.
 * <p>
 * The interface rides two blocks out from the axle rather than one, which is what lets it dock at all.
 * An interface looks for one to dock with only as it crosses into a new block, and it only accepts a
 * facing that still points nearly along an axis. One block out, those never coincide: the crossing
 * falls on the diagonal, where the facing is refused. Two blocks out the crossing comes early enough in
 * the turn for the facing to still be good.
 * <p>
 * It starts pointed at neither tank, so a run shows the pick-up and the drop-off rather than beginning
 * already docked.
 */
public class PortableFluidTransportTest {

	/** The height the bearing turns at, with room under it for its motor. */
	private static final int AXLE_Y = -58;

	/** Slow enough to watch it come round and dock. */
	private static final int BEARING_RPM = 16;
	private static final int PUMP_RPM = 64;

	/** A little water, as asked - well under what the travelling tank could hold. */
	private static final int WATER = 1000;

	private static final int PATIENCE_TICKS = 1200;

	/** Which way along the line an end stands: the full tank one way, the empty one the other. */
	private static final int FULL_SIDE = 1;
	private static final int EMPTY_SIDE = -1;

	@ClientGameTest(screenshot = false)
	@DisplayName("A turning contraption carries water from one tank to the other")
	void carriesWaterBetweenTanks(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		watchTheBearing(context, server);
		clearTheGround(server);
		build(server);

		ServerLevel[] world = new ServerLevel[1];

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			world[0] = level;

			// The glue is what makes the three of them one contraption, and it has to be there before the
			// bearing is turned on, since turning it on is what assembles them.
			level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(carriedTank(), carriedPort())));

			fill(level, tank(FULL_SIDE), WATER);

			turn(level, bearingMotor(), BEARING_RPM);
			turn(level, pumpMotor(FULL_SIDE), PUMP_RPM);
			turn(level, pumpMotor(EMPTY_SIDE), PUMP_RPM);
		});

		context.waitTicks(20);
		showFluid(context, world, tank(FULL_SIDE), "from");
		showFluid(context, world, carriedTank(), "carried");
		showFluid(context, world, tank(EMPTY_SIDE), "to");
		context.takeScreenshot(shot("portable_fluid_before"));

		assertTrue(contraptionAssembled(server), "The bearing did not assemble anything at all");

		// A block that joined the contraption is no longer a block in the world, so this is how to tell
		// that the glue held and all of them went round together.
		assertTrue(joinedTheContraption(server, carriedTank()), "The tank did not join the contraption");
		assertTrue(joinedTheContraption(server, carriedPort()),
			"The interface did not join the contraption, so the glue did not hold");

		int waited = 0;

		while (waited < PATIENCE_TICKS && held(server, tank(EMPTY_SIDE)) < WATER) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("portable_fluid_after"));
		context.clearOverlays();

		assertEquals(WATER, held(server, tank(EMPTY_SIDE)),
			"The water the contraption carried round did not all reach the far tank");
		assertEquals(0, held(server, tank(FULL_SIDE)), "The tank the water came from was not emptied");
	}

	private void build(TestServerContext server) {
		setBlock(server, bearing(), "create:mechanical_bearing[facing=up]");
		setBlock(server, bearingMotor(), "create:creative_motor[facing=up]");

		// What turns: a tank on the bearing, a casing reaching out past it, and the interface at the end.
		// The arm lies across the line the fixed interfaces stand on, so it starts facing neither of them
		// and comes round to each a quarter turn later.
		setBlock(server, carriedTank(), "create:fluid_tank");
		setBlock(server, carriedArm(), "create:andesite_casing");
		setBlock(server, carriedPort(), "create:portable_fluid_interface[facing=east]");

		plumb(server, FULL_SIDE);
		plumb(server, EMPTY_SIDE);
	}

	/**
	 * One end of the line: the fixed interface the contraption docks with, the pump that moves water
	 * through it, and the tank beyond.
	 */
	private void plumb(TestServerContext server, int side) {
		setBlock(server, fixedPort(side),
			"create:portable_fluid_interface[facing=%s]".formatted(side > 0 ? "north" : "south"));

		// Both pumps face north, so on the full side the water is pushed in towards the bearing and on
		// the empty side onwards to the tank - always the same way along the line.
		setBlock(server, pump(side), "create:mechanical_pump[facing=north]");
		setBlock(server, pump(side).above(), "create:cogwheel[axis=z]");
		setBlock(server, pumpMotor(side),
			"create:creative_motor[facing=%s]".formatted(side > 0 ? "south" : "north"));
		setBlock(server, tank(side), "create:fluid_tank");
	}

	private BlockPos bearing() {
		return new BlockPos(0, AXLE_Y, 0);
	}

	private BlockPos bearingMotor() {
		return new BlockPos(0, AXLE_Y - 1, 0);
	}

	/** The tank that goes round, sitting on the bearing itself. */
	private BlockPos carriedTank() {
		return new BlockPos(0, AXLE_Y + 1, 0);
	}

	/** What reaches out from it, so that the interface rides two blocks from the axle. */
	private BlockPos carriedArm() {
		return new BlockPos(1, AXLE_Y + 1, 0);
	}

	private BlockPos carriedPort() {
		return new BlockPos(2, AXLE_Y + 1, 0);
	}

	/**
	 * Where the fixed interface stands: two blocks out from where the turning one comes to rest, so that
	 * a block of air is left between the two of them, which is the distance they reach across.
	 */
	private BlockPos fixedPort(int side) {
		return new BlockPos(0, AXLE_Y + 1, 4 * side);
	}

	private BlockPos pump(int side) {
		return new BlockPos(0, AXLE_Y + 1, 5 * side);
	}

	private BlockPos pumpMotor(int side) {
		return new BlockPos(0, AXLE_Y + 2, 4 * side);
	}

	private BlockPos tank(int side) {
		return new BlockPos(0, AXLE_Y + 1, 6 * side);
	}

	/** Whether the bearing turned the glued blocks into something that moves. */
	private boolean contraptionAssembled(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> !minecraftServer.overworld()
			.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(bearing()).inflate(6))
			.isEmpty());
	}

	/** Whether the block that was here has been taken up into the contraption. */
	private boolean joinedTheContraption(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockState(pos)
			.isAir());
	}

	/**
	 * Notes how much water a tank is holding beside it.
	 * <p>
	 * A tank is not an item container, so the ready-made note for those has nothing to say about one.
	 */
	private void showFluid(ClientGameTestContext context, ServerLevel[] world, BlockPos tank, String label) {
		context.showOverlay(tank, () -> {
			if (world[0] == null || !(world[0].getBlockEntity(tank) instanceof FluidTankBlockEntity be))
				return label + ": no tank";

			return label + ": " + be.getControllerBE()
				.getTankInventory()
				.getFluidAmount();
		});
	}

	private void fill(ServerLevel level, BlockPos tank, int amount) {
		((FluidTankBlockEntity) level.getBlockEntity(tank)).getControllerBE()
			.getTankInventory()
			.setFluid(new FluidStack(Fluids.WATER, amount));
	}

	/** How much fluid a tank is holding, on the server. */
	private int held(TestServerContext server, BlockPos tank) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(tank) instanceof FluidTankBlockEntity be))
				return 0;

			return be.getControllerBE()
				.getTankInventory()
				.getFluidAmount();
		});
	}

	private void turn(ServerLevel level, BlockPos motor, int rpm) {
		((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(rpm);
	}

	private void clearTheGround(TestServerContext server) {
		server.runCommand("fill -4 %d -8 4 %d 8 air".formatted(AXLE_Y - 2, AXLE_Y + 4));
		server.runCommand("kill @e[type=item]");
	}

	private static void watchTheBearing(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 15.0 %d 0.0 facing 0.0 %d 0.0".formatted(AXLE_Y + 5, AXLE_Y + 1));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
