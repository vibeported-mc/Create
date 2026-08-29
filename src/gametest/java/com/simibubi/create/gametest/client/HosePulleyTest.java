package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

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

/**
 * Two hose pulleys emptying one glass tank into another, for water and for lava.
 * <p>
 * This is the other half of Create's fluid handling from {@link FluidTransferTest}: rather than
 * moving fluid between two of the mod's own tanks, a hose pulley reaches down into a body of fluid in
 * the world and drains it, and a second one on the far end of the pipe puts it back. The tanks are
 * built out of glass so that both the fluid and the hoses hanging in it can be seen.
 * <p>
 * <b>Disabled: this does not work in the port.</b> The test is here because it pins down where.
 * <p>
 * The scene is right - both hoses reach the bottom, the pipe beside the source pulley reports a
 * connection towards it, and the pulley hands out its fluid capability on that face. The pipe builds
 * a flow source for it, and that source holds the pulley's handler.
 * <p>
 * What goes wrong is on the taking side. The source pulley is only ever asked to <i>simulate</i>
 * draining a single millibucket, which is the network looking to see what is there; the real,
 * committed extraction never arrives, so the transaction that would remove a block from the pool is
 * never committed and the pool stays full. The far pulley meanwhile does commit its deposits, so
 * fluid appears in the second tank without leaving the first - nine blocks of it, one full layer,
 * before it stops.
 * <p>
 * Upstream's own 26.2 port takes the block inline, in the same call that reports what is available,
 * rather than deferring it to a transaction commit the way this port does. That difference is the
 * place to start.
 */
public class HosePulleyTest {

	/** The floor the glass tanks stand on. */
	private static final int FLOOR = -61;

	/** How far across the inside of a tank is, and how deep. */
	private static final int INSIDE = 3;

	/** The pulleys and the pipe between them sit above the open tops of the tanks. */
	private static final int PIPE_LEVEL = FLOOR + INSIDE + 2;

	/** The pump runs flat out; it moves fluid in proportion to how fast it turns. */
	private static final int PUMP_SPEED = 256;

	/** The pulleys are wound down gently, so a hose stops where it is meant to. */
	private static final int PULLEY_SPEED = 256;

	/** Long enough at that speed for a hose to reach the floor of a tank. */
	private static final int LOWERING_TICKS = 60;

	private static final int PATIENCE_TICKS = 900;

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

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			water.startPump(level);
			lava.startPump(level);
			water.lowerTheHoses(level);
			lava.lowerTheHoses(level);
		});

		watchTheWholeThing(context, server);
		context.waitTicks(LOWERING_TICKS);

		// A pulley will not move fluid while its hose is still travelling, so once both hoses are at
		// the bottom the motors that lowered them are taken away. The pump keeps turning.
		water.removeTheHoseMotors(server);
		lava.removeTheHoseMotors(server);

		context.waitTicks(20);
		context.takeScreenshot(shot("hose_pulley_before"));

		int startingWater = water.inSource(server);
		int startingLava = lava.inSource(server);

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

		assertEquals(INSIDE * INSIDE * INSIDE, startingWater, "The water tank did not start full");
		assertEquals(INSIDE * INSIDE * INSIDE, startingLava, "The lava tank did not start full");
		assertTrue(waterMoved > 0, "No water reached the far tank within " + waited + " ticks");
		assertTrue(lavaMoved > 0, "No lava reached the far tank within " + waited + " ticks");
		assertTrue(waterLeft < startingWater, "The near water tank was never drained");
		assertTrue(lavaLeft < startingLava, "The near lava tank was never drained");
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
			BlockPos high = inside.offset(INSIDE, INSIDE, INSIDE);

			fill(server, low, high, "minecraft:glass");
			fill(server, inside, inside.offset(INSIDE - 1, INSIDE - 1, INSIDE - 1), "minecraft:air");
			// Open at the top so a hose can be let down into it.
			fill(server, new BlockPos(low.getX(), high.getY(), low.getZ()), high, "minecraft:air");

			if (inside.equals(sourceInside()))
				fill(server, inside, inside.offset(INSIDE - 1, INSIDE - 1, INSIDE - 1),
					fluid == Fluids.LAVA ? "minecraft:lava" : "minecraft:water");
		}

		void startPump(ServerLevel level) {
			// Negative here: a cog reverses what it meshes with, and a pump moves fluid the way it is
			// turning rather than the way it faces.
			motor(level, pumpMotor(), -PUMP_SPEED);
		}

		void lowerTheHoses(ServerLevel level) {
			motor(level, sourcePulley().west(), PULLEY_SPEED);
			// The far motor faces the other way, so the same speed would wind its hose up rather than
			// down. Both hoses have to reach the bottom of their tank.
			motor(level, destinationPulley().east(), -PULLEY_SPEED);
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
					for (int dy = 0; dy < INSIDE; dy++)
						for (int dz = 0; dz < INSIDE; dz++)
							if (level.getFluidState(inside.offset(dx, dy, dz))
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
