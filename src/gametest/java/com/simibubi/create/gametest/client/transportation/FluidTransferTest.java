package com.simibubi.create.gametest.client.transportation;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * A mechanical pump emptying one Create fluid tank into another, twice over: water between a pair of
 * two by two tanks, and lava between a pair of three by three ones, three being as wide as a tank
 * goes.
 * <p>
 * Both runs are built at once, in one line so that neither hides the other, and are left to work side
 * by side.
 */
public class FluidTransferTest {

	/** The floor the whole thing stands on. */
	private static final int GROUND = -60;

	/** Enough to be obvious in the tanks without filling either of them to the brim. */
	private static final int AMOUNT = 16000;

	/** As fast as a creative motor goes, so the pump gets through the transfer briskly. */
	private static final int MOTOR_SPEED = 256;

	private static final int PATIENCE_TICKS = 600;

	@ClientGameTest(screenshot = false)
	@DisplayName("A mechanical pump moves water and lava between tanks")
	void pumpBetweenTanks(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		Run water = new Run(Fluids.WATER, new BlockPos(0, GROUND, 0), 2);
		Run lava = new Run(Fluids.LAVA, new BlockPos(10, GROUND, 0), 3);

		water.build(server);
		lava.build(server);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			water.windUpTheMotor(level);
			lava.windUpTheMotor(level);
			water.fillSource(level);
			lava.fillSource(level);
		});

		watchTheWholeThing(context, server);
		context.waitTicks(20);
		context.takeScreenshot(shot("fluid_transfer_before"));

		// Waits for both source tanks to run dry rather than for the first drop to arrive, so what is
		// checked below is the whole transfer and not the fact that the pump twitched once.
		int waited = 0;
		while (waited < PATIENCE_TICKS && (water.remaining(server) > 0 || lava.remaining(server) > 0)) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("fluid_transfer_after"));

		int waterMoved = water.moved(server);
		int lavaMoved = lava.moved(server);

		// Restored before the assertions, so a failure does not leave the hud hidden for whatever runs
		// next.
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		assertEquals(AMOUNT, waterMoved,
			"The water did not all reach the far tank within " + waited + " ticks");
		assertEquals(AMOUNT, lavaMoved,
			"The lava did not all reach the far tank within " + waited + " ticks");
		assertEquals(0, water.remaining(server), "Water was left behind in the near tank");
		assertEquals(0, lava.remaining(server), "Lava was left behind in the near tank");
	}

	/**
	 * One source tank, a pump, and one destination tank, all in a line running east.
	 *
	 * @param width how many blocks across the tanks are, which is also how tall they are built here
	 */
	private record Run(Fluid fluid, BlockPos corner, int width) {

		/** The first tank starts at the corner; the second one begins two blocks past its far side. */
		BlockPos destinationCorner() {
			return corner.offset(width + 3, 0, 0);
		}

		/** The pipe run sits on the bottom layer, in the middle row of the tanks. */
		private int lane() {
			return corner.getZ() + width / 2;
		}

		void build(TestServerContext server) {
			tank(server, corner);
			tank(server, destinationCorner());

			int pipeX = corner.getX() + width;
			setBlock(server, new BlockPos(pipeX, GROUND, lane()), "create:fluid_pipe");
			// The pump pushes the way it faces, so east is from the first tank towards the second.
			setBlock(server, new BlockPos(pipeX + 1, GROUND, lane()), "create:mechanical_pump[facing=east]");
			setBlock(server, new BlockPos(pipeX + 2, GROUND, lane()), "create:fluid_pipe");

			// A pump is a cogwheel, so a cog beside it turns it, and a motor on the same axis turns the
			// cog. All three share the axis the pump faces along.
			setBlock(server, new BlockPos(pipeX + 1, GROUND + 1, lane()), "create:cogwheel[axis=x]");
			setBlock(server, new BlockPos(pipeX, GROUND + 1, lane()), "create:creative_motor[facing=east]");
		}

		private void tank(TestServerContext server, BlockPos start) {
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					for (int y = 0; y < width; y++)
						setBlock(server, start.offset(x, y, z), "create:fluid_tank");
		}

		/**
		 * A pump moves fluid in proportion to how fast it turns, and a creative motor idles at sixteen
		 * revolutions a minute, which would take this test a minute and a half. Wound up to its limit
		 * the same transfer takes a few seconds.
		 */
		void windUpTheMotor(ServerLevel level) {
			BlockPos motor = new BlockPos(corner.getX() + width, GROUND + 1, lane());
			((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(MOTOR_SPEED);
		}

		void fillSource(ServerLevel level) {
			tankAt(level, corner).getTankInventory()
				.setFluid(new FluidStack(fluid, AMOUNT));
		}

		int moved(TestServerContext server) {
			return server.computeOnServer(minecraftServer -> contents(minecraftServer.overworld(),
				destinationCorner()));
		}

		int remaining(TestServerContext server) {
			return server.computeOnServer(minecraftServer -> contents(minecraftServer.overworld(), corner));
		}

		private int contents(ServerLevel level, BlockPos start) {
			return tankAt(level, start).getTankInventory()
				.getFluidAmount();
		}

		/**
		 * The tank a position belongs to, which for a multiblock is whichever of its blocks holds the
		 * fluid for all of them.
		 */
		private FluidTankBlockEntity tankAt(ServerLevel level, BlockPos pos) {
			return ((FluidTankBlockEntity) level.getBlockEntity(pos)).getControllerBE();
		}
	}

	/**
	 * A raised three quarter view from the south east, far enough out that both runs are in frame and
	 * the fluid in every tank can be seen.
	 */
	private static void watchTheWholeThing(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		// Everything stands in one line running east, from the water pair at x=0 to the far lava tank
		// at x=18.
		double centreX = 9.5;
		double centreY = GROUND + 1.5;
		double centreZ = 1.0;

		double eyeX = centreX + 2;
		double eyeY = centreY + 5;
		double eyeZ = centreZ + 16;

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

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
