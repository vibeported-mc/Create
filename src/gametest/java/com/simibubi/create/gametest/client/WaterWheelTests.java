package com.simibubi.create.gametest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Water wheels driven by a creative motor, photographed.
 * <p>
 * A client gametest runs the real client, so these cover rather more than the blocks they place:
 * world creation, Create's registration, a kinetic network forming across a block boundary, and
 * Flywheel drawing the result.
 */
public class WaterWheelTests {

	/**
	 * The superflat preset the test world builder uses puts grass at y=-61, so the first free block
	 * above the ground is y=-60.
	 */
	private static final int GROUND = -60;

	@ClientGameTest
	@DisplayName("A creative motor turns a small water wheel")
	void smallWaterWheel(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getClientLevel().waitForChunksRender();

		BlockPos wheel = new BlockPos(3, GROUND, 0);
		// The motor sits on the wheel's north face, where the wheel's shaft comes out. facing is the
		// axis the shaft runs along, so both blocks face along z and meet.
		BlockPos motor = wheel.north();

		TestServerContext server = singleplayer.getServer();
		setBlock(server, wheel, "create:water_wheel[facing=north]");
		setBlock(server, motor, "create:creative_motor[facing=south]");
		lookAt(server, wheel);

		// Long enough for the network to form and the wheel to spin up to the motor's speed.
		context.waitTicks(40);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			assertBlock(level, wheel, AllBlocks.WATER_WHEEL);
			assertBlock(level, motor, AllBlocks.CREATIVE_MOTOR);
			assertDriven(level, wheel, motor);
		});

		context.takeScreenshot("water_wheel");
	}

	@ClientGameTest
	@DisplayName("A creative motor turns a large water wheel")
	void largeWaterWheel(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getClientLevel().waitForChunksRender();

		// The large wheel is 3x3 around its centre, so it has to stand a block clear of the ground.
		BlockPos wheel = new BlockPos(3, GROUND + 1, 0);
		BlockPos motor = wheel.north();

		TestServerContext server = singleplayer.getServer();
		setBlock(server, wheel, "create:large_water_wheel[axis=z]");
		setBlock(server, motor, "create:creative_motor[facing=south]");
		lookAt(server, wheel);

		context.waitTicks(40);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			assertBlock(level, wheel, AllBlocks.LARGE_WATER_WHEEL);
			assertBlock(level, motor, AllBlocks.CREATIVE_MOTOR);

			// The large wheel builds its own ring of structural blocks on the tick after placement,
			// and tears itself down again if anything is in the way, so their presence is the real
			// evidence that it stood up.
			assertBlock(level, wheel.above(), AllBlocks.WATER_WHEEL_STRUCTURAL);
			assertBlock(level, wheel.below(), AllBlocks.WATER_WHEEL_STRUCTURAL);

			assertDriven(level, wheel, motor);
		});

		context.takeScreenshot("large_water_wheel");
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

	/** Stands off the south-east corner so the shot catches the wheel's face and the motor beside it. */
	private static void lookAt(TestServerContext server, BlockPos pos) {
		server.runCommand("tp @a %.1f %d %.1f 141 10".formatted(pos.getX() + 4.5, GROUND, pos.getZ() + 4.5));
	}

	private static void assertBlock(ServerLevel level, BlockPos pos, BlockEntry<?> expected) {
		BlockState state = level.getBlockState(pos);
		assertTrue(expected.has(state), () -> "Expected " + expected.getId() + " at " + pos + ", found " + state);
	}

	private static void assertDriven(ServerLevel level, BlockPos wheelPos, BlockPos motorPos) {
		KineticBlockEntity wheel = kineticAt(level, wheelPos);
		KineticBlockEntity motor = kineticAt(level, motorPos);

		// A water wheel generates nothing without water, so any speed it has comes from the motor.
		assertNotEquals(0f, wheel.getSpeed(), "The water wheel is not turning, so the motor did not reach it");
		assertEquals(motor.network, wheel.network, "The wheel and the motor are on separate kinetic networks");

		// The two are shaft to shaft with no gearing between them to change the rate. Which way round
		// the sign comes out is the propagator's business, so only the rate is checked.
		assertEquals(Math.abs(motor.getSpeed()), Math.abs(wheel.getSpeed()),
			"The wheel is not turning at the motor's speed");
	}

	private static KineticBlockEntity kineticAt(ServerLevel level, BlockPos pos) {
		return assertInstanceOf(KineticBlockEntity.class, level.getBlockEntity(pos),
			"Expected a kinetic block entity at " + pos);
	}

}
