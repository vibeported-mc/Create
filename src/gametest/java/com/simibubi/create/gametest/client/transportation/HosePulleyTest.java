package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Two hose pulleys emptying one glass tank into another, for water and for lava.
 * <p>
 * This is the other half of Create's fluid handling from {@link FluidTransferTest}: rather than
 * moving fluid between two of the mod's own tanks, a hose pulley reaches down into a body of fluid in
 * the world and drains it, and a second one on the far end of the pipe puts it back. The tanks are
 * built out of glass so that both the fluid and the hoses hanging in it can be seen.
 * <p>
 * A second test sends fluid between a glass tank and one of the mod's own, in both directions, so
 * that the pulley is exercised as a source drawing into a real tank and as a destination fed by
 * one.
 */
public class HosePulleyTest {

	/** The floor the glass tanks stand on. */
	private static final int FLOOR = -61;

	/** How far across the inside of a tank is. */
	private static final int INSIDE = 3;

	/** The pulleys and the pipe between them sit level with the open tops of the tanks. */
	private static final int PIPE_LEVEL = FLOOR + INSIDE + 2;

	/** The inside runs from just above the floor up to just below the pulleys. */
	private static final int HEIGHT = PIPE_LEVEL - 1 - (FLOOR + 1) + 1;

	/** The pump runs flat out; it moves fluid in proportion to how fast it turns. */
	private static final int PUMP_SPEED = 256;

	/** The near hose is wound all the way down into the pool it is draining. */
	private static final int NEAR_PULLEY_SPEED = 256;

	/**
	 * The far hose has to stop one block down: at rest it ends inside the pulley itself with nowhere
	 * to put anything, and let all the way down it fills the floor of the tank and gets no further.
	 * <p>
	 * A hose does not stop where the speed says, it stops where something blocks it, so a stop is
	 * arranged rather than timed: a block is put in its way, and taken out once the hose has settled
	 * against it.
	 */
	private static final int FAR_PULLEY_SPEED = 64;

	/** Long enough at that speed for a hose to reach the floor of a tank. */
	private static final int LOWERING_TICKS = 60;



	private static final int PATIENCE_TICKS = 900;

	/** What the Create tank is given when it is the one being drained. */
	private static final int TANK_AMOUNT = 24000;

	@ClientGameTest(screenshot = false)
	@DisplayName("Hose pulleys drain one glass tank into another")
	void pulleyToPulley(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		Run water = new Run(Fluids.WATER, 0);
		Run lava = new Run(Fluids.LAVA, 16);

		water.build(server);
		lava.build(server);

		int startingWater = water.inSource(server);
		int startingLava = lava.inSource(server);

		water.blockTheFarHose(server);
		lava.blockTheFarHose(server);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			water.startPump(level);
			lava.startPump(level);
			water.lowerTheHoses(level);
			lava.lowerTheHoses(level);
		});

		watchTheWholeThing(context, server);

		// A pulley moves no fluid while its hose is still travelling, so each motor is taken away once
		// its hose is where it should be. The pump keeps turning.
		context.waitTicks(LOWERING_TICKS);
		water.removeTheHoseMotors(server);
		lava.removeTheHoseMotors(server);
		water.clearTheFarHoseStop(server);
		lava.clearTheFarHoseStop(server);

		context.waitTicks(20);
		context.takeScreenshot(shot("hose_pulley_before"));

		int waited = 0;
		while (waited < PATIENCE_TICKS && (water.inSource(server) > 0 || lava.inSource(server) > 0)) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("hose_pulley_after"));

		int waterMoved = water.inDestination(server);
		int lavaMoved = lava.inDestination(server);
		int waterLeft = water.inSource(server);
		int lavaLeft = lava.inSource(server);

		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		assertEquals(INSIDE * INSIDE * HEIGHT, startingWater, "The water tank did not start full");
		assertEquals(INSIDE * INSIDE * HEIGHT, startingLava, "The lava tank did not start full");
		assertTrue(waterMoved > 0, "No water reached the far tank within " + waited + " ticks");
		assertTrue(lavaMoved > 0, "No lava reached the far tank within " + waited + " ticks");
		assertEquals(0, waterLeft, "The near water tank was not emptied");
		assertEquals(0, lavaLeft, "The near lava tank was not emptied");
	}

	/**
	 * A pair of glass tanks with a hose pulley over each, joined by a pipe with a pump in the middle.
	 *
	 * @param x where the inside of the first tank starts; everything else follows from it
	 */
	private record Run(Fluid fluid, int x) {

		/** The inside of the tank the fluid starts in. */
		BlockPos sourceInside() {
			return new BlockPos(x, FLOOR + 1, 0);
		}

		/** The inside of the tank it should end up in, six blocks further east. */
		BlockPos destinationInside() {
			return new BlockPos(x + INSIDE + 5, FLOOR + 1, 0);
		}

		BlockPos sourcePulley() {
			return new BlockPos(x + 1, PIPE_LEVEL, 1);
		}

		BlockPos destinationPulley() {
			return new BlockPos(destinationInside().getX() + 1, PIPE_LEVEL, 1);
		}

		/** Where the far hose is stopped: one below the block it should come to rest in. */
		private BlockPos farHoseStop() {
			return destinationPulley().below(2);
		}

		void blockTheFarHose(TestServerContext server) {
			setBlock(server, farHoseStop(), "minecraft:glass");
		}

		void clearTheFarHoseStop(TestServerContext server) {
			setBlock(server, farHoseStop(), "minecraft:air");
		}

		void build(TestServerContext server) {
			glassTank(server, sourceInside());
			glassTank(server, destinationInside());

			// A pulley carries its shaft on one side and its pipe on the other, so the two face
			// opposite ways: the pipes point at each other and the motors sit on the outside.
			setBlock(server, sourcePulley(), "create:hose_pulley[facing=south]");
			setBlock(server, destinationPulley(), "create:hose_pulley[facing=north]");
			setBlock(server, sourcePulley().west(), "create:creative_motor[facing=east]");
			setBlock(server, destinationPulley().east(), "create:creative_motor[facing=west]");

			// Pipe between the two, with a pump partway along pushing east.
			int from = sourcePulley().getX() + 1;
			int to = destinationPulley().getX() - 1;
			int pump = (from + to) / 2;

			for (int px = from; px <= to; px++)
				setBlock(server, new BlockPos(px, PIPE_LEVEL, 1),
					px == pump ? "create:mechanical_pump[facing=east]" : "create:fluid_pipe");

			// The pump is a cogwheel; a cog above it and a motor beside that turn it.
			setBlock(server, new BlockPos(pump, PIPE_LEVEL + 1, 1), "create:cogwheel[axis=x]");
			setBlock(server, new BlockPos(pump - 1, PIPE_LEVEL + 1, 1), "create:creative_motor[facing=east]");
		}

		/**
		 * A glass box open at the top. The first one is filled to the brim, the second left empty.
		 */
		private void glassTank(TestServerContext server, BlockPos inside) {
			BlockPos low = inside.offset(-1, -1, -1);
			BlockPos high = inside.offset(INSIDE, HEIGHT, INSIDE);
			BlockPos insideHigh = inside.offset(INSIDE - 1, HEIGHT - 1, INSIDE - 1);

			fill(server, low, high, "minecraft:glass");
			fill(server, inside, insideHigh, "minecraft:air");
			// Open at the top, level with the pulleys, so a hose hanging at rest is already inside.
			fill(server, new BlockPos(low.getX(), high.getY(), low.getZ()), high, "minecraft:air");

			if (inside.equals(sourceInside()))
				fill(server, inside, insideHigh, fluid == Fluids.LAVA ? "minecraft:lava" : "minecraft:water");
		}

		void startPump(ServerLevel level) {
			// Negative here: a cog reverses what it meshes with, and a pump moves fluid the way it is
			// turning rather than the way it faces.
			motor(level, pumpMotor(), -PUMP_SPEED);
		}

		void lowerTheHoses(ServerLevel level) {
			motor(level, sourcePulley().west(), NEAR_PULLEY_SPEED);
			// Negative because the far motor faces the other way.
			motor(level, destinationPulley().east(), -FAR_PULLEY_SPEED);
		}

		void removeTheHoseMotors(TestServerContext server) {
			setBlock(server, sourcePulley().west(), "minecraft:air");
			setBlock(server, destinationPulley().east(), "minecraft:air");
		}

		BlockPos pumpPos() {
			return new BlockPos((sourcePulley().getX() + 1 + destinationPulley().getX() - 1) / 2, PIPE_LEVEL, 1);
		}

		private BlockPos pumpMotor() {
			int pump = (sourcePulley().getX() + 1 + destinationPulley().getX() - 1) / 2;
			return new BlockPos(pump - 1, PIPE_LEVEL + 1, 1);
		}

		private void motor(ServerLevel level, BlockPos pos, int speed) {
			((CreativeMotorBlockEntity) level.getBlockEntity(pos)).generatedSpeed.setValue(speed);
		}

		int inSource(TestServerContext server) {
			return count(server, sourceInside());
		}

		int inDestination(TestServerContext server) {
			return count(server, destinationInside());
		}

		/** How many blocks of this run's fluid are standing inside the given tank. */
		int count(TestServerContext server, BlockPos inside) {
			return server.computeOnServer(minecraftServer -> {
				ServerLevel level = minecraftServer.overworld();
				int found = 0;

				for (int dx = 0; dx < INSIDE; dx++)
					for (int dy = 0; dy < HEIGHT; dy++)
						for (int dz = 0; dz < INSIDE; dz++)
							if (level.getFluidState(inside.offset(dx, dy, dz))
								.getType()
								.isSame(fluid))
								found++;

				return found;
			});
		}
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("A hose pulley drains into a Create tank, and fills a pool from one")
	void glassAndCreateTanks(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		Mixed draining = new Mixed(Fluids.WATER, 0, true);
		Mixed filling = new Mixed(Fluids.LAVA, 18, false);

		draining.build(server);
		filling.build(server);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			draining.start(level);
			filling.start(level);
		});

		watchTheWholeThing(context, server);
		context.waitTicks(LOWERING_TICKS);
		draining.settle(server);
		filling.settle(server);

		context.waitTicks(20);
		context.takeScreenshot(shot("hose_pulley_tanks_before"));

		int waited = 0;
		while (waited < PATIENCE_TICKS && (draining.inSource(server) > 0 || filling.inSource(server) > 0)) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("hose_pulley_tanks_after"));

		int drainedLeft = draining.inSource(server);
		int drainedInto = draining.inDestination(server);
		int filledLeft = filling.inSource(server);
		int filledInto = filling.inDestination(server);

		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		assertEquals(0, drainedLeft, "The pool was not emptied into the Create tank");
		assertTrue(drainedInto > 0, "Nothing reached the Create tank within " + waited + " ticks");
		assertEquals(0, filledLeft, "The Create tank was not emptied into the pool");
		assertTrue(filledInto > 0, "Nothing reached the pool within " + waited + " ticks");
	}

	/**
	 * A glass tank and one of the mod's own, joined by a pipe with a pump between them.
	 *
	 * @param fromThePool true when the pulley is draining the glass tank into the Create tank, false
	 *                    when it is the far end filling the glass tank from one
	 */
	private record Mixed(Fluid fluid, int x, boolean fromThePool) {

		/** The glass tank stands at the pulley end of the line and the Create tank at the other. */
		private BlockPos poolInside() {
			return new BlockPos(fromThePool ? x : x + 6, FLOOR + 1, 0);
		}

		private BlockPos pulley() {
			return new BlockPos(poolInside().getX() + 1, PIPE_LEVEL, 1);
		}

		private BlockPos createTank() {
			return new BlockPos(fromThePool ? x + 6 : x, PIPE_LEVEL - 2, 0);
		}

		private BlockPos motor() {
			return fromThePool ? pulley().west() : pulley().east();
		}

		/** Where the hose is stopped when this pulley is the one doing the filling. */
		private BlockPos hoseStop() {
			return pulley().below(2);
		}

		private int pumpX() {
			int from = fromThePool ? pulley().getX() + 1 : createTank().getX() + 3;
			int to = fromThePool ? createTank().getX() - 1 : pulley().getX() - 1;
			return (from + to) / 2;
		}

		void build(TestServerContext server) {
			glassTank(server);

			for (int dx = 0; dx < 3; dx++)
				for (int dy = 0; dy < 3; dy++)
					for (int dz = 0; dz < 3; dz++)
						setBlock(server, createTank().offset(dx, dy, dz), "create:fluid_tank");

			// A pulley carries its shaft on one side and its pipe on the other, so which way it faces
			// decides which end of the line it belongs to.
			setBlock(server, pulley(), fromThePool ? "create:hose_pulley[facing=south]"
				: "create:hose_pulley[facing=north]");
			setBlock(server, motor(), fromThePool ? "create:creative_motor[facing=east]"
				: "create:creative_motor[facing=west]");

			int from = fromThePool ? pulley().getX() + 1 : createTank().getX() + 3;
			int to = fromThePool ? createTank().getX() - 1 : pulley().getX() - 1;

			for (int px = from; px <= to; px++)
				setBlock(server, new BlockPos(px, PIPE_LEVEL, 1),
					px == pumpX() ? "create:mechanical_pump[facing=east]" : "create:fluid_pipe");

			setBlock(server, new BlockPos(pumpX(), PIPE_LEVEL + 1, 1), "create:cogwheel[axis=x]");
			setBlock(server, new BlockPos(pumpX() - 1, PIPE_LEVEL + 1, 1),
				"create:creative_motor[facing=east]");

			// A hose at rest ends inside the pulley itself, and let all the way down it fills only the
			// floor, so the filling one is stopped a block down by putting something in its way.
			if (!fromThePool)
				setBlock(server, hoseStop(), "minecraft:glass");
		}

		private void glassTank(TestServerContext server) {
			BlockPos inside = poolInside();
			BlockPos low = inside.offset(-1, -1, -1);
			BlockPos high = inside.offset(INSIDE, HEIGHT, INSIDE);
			BlockPos insideHigh = inside.offset(INSIDE - 1, HEIGHT - 1, INSIDE - 1);

			fill(server, low, high, "minecraft:glass");
			fill(server, inside, insideHigh, "minecraft:air");
			fill(server, new BlockPos(low.getX(), high.getY(), low.getZ()), high, "minecraft:air");

			if (fromThePool)
				fill(server, inside, insideHigh, fluid == Fluids.LAVA ? "minecraft:lava" : "minecraft:water");
		}

		void start(ServerLevel level) {
			((CreativeMotorBlockEntity) level.getBlockEntity(new BlockPos(pumpX() - 1, PIPE_LEVEL + 1, 1)))
				.generatedSpeed.setValue(-PUMP_SPEED);
			((CreativeMotorBlockEntity) level.getBlockEntity(motor())).generatedSpeed
				.setValue(fromThePool ? NEAR_PULLEY_SPEED : -NEAR_PULLEY_SPEED);

			if (!fromThePool)
				((FluidTankBlockEntity) level.getBlockEntity(createTank())).getControllerBE()
					.getTankInventory()
					.setFluid(new FluidStack(fluid, TANK_AMOUNT));
		}

		void settle(TestServerContext server) {
			setBlock(server, motor(), "minecraft:air");

			if (!fromThePool)
				setBlock(server, hoseStop(), "minecraft:air");
		}

		/** Blocks in the pool, or millibuckets in the Create tank, whichever holds the source. */
		int inSource(TestServerContext server) {
			return fromThePool ? poolBlocks(server) : tankAmount(server);
		}

		int inDestination(TestServerContext server) {
			return fromThePool ? tankAmount(server) : poolBlocks(server);
		}

		private int tankAmount(TestServerContext server) {
			return server.computeOnServer(minecraftServer -> ((FluidTankBlockEntity) minecraftServer
				.overworld()
				.getBlockEntity(createTank())).getControllerBE()
					.getTankInventory()
					.getFluidAmount());
		}

		private int poolBlocks(TestServerContext server) {
			return server.computeOnServer(minecraftServer -> {
				ServerLevel level = minecraftServer.overworld();
				int found = 0;

				for (int dx = 0; dx < INSIDE; dx++)
					for (int dy = 0; dy < HEIGHT; dy++)
						for (int dz = 0; dz < INSIDE; dz++)
							if (level.getFluidState(poolInside().offset(dx, dy, dz))
								.getType()
								.isSame(fluid))
								found++;

				return found;
			});
		}
	}

	private static void watchTheWholeThing(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		double centreX = 12.5;
		double centreY = FLOOR + 3;
		double centreZ = 1.0;

		double eyeX = centreX + 2;
		double eyeY = centreY + 5;
		double eyeZ = centreZ + 14;

		double toX = centreX - eyeX;
		double toY = centreY - eyeY;
		double toZ = centreZ - eyeZ;
		double flat = Math.sqrt(toX * toX + toZ * toZ);

		server.runCommand("gamemode spectator @a");

		double eyeHeight = server.computeOnServer(minecraftServer -> (double) minecraftServer
			.getPlayerList()
			.getPlayers()
			.getFirst()
			.getEyeHeight());

		server.runCommand("tp @a %.2f %.2f %.2f %.2f %.2f".formatted(eyeX, eyeY - eyeHeight, eyeZ,
			Math.toDegrees(Math.atan2(-toX, toZ)), Math.toDegrees(-Math.atan2(toY, flat))));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void fill(TestServerContext server, BlockPos low, BlockPos high, String block) {
		server.runCommand("fill %d %d %d %d %d %d %s".formatted(low.getX(), low.getY(), low.getZ(),
			high.getX(), high.getY(), high.getZ(), block));
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
