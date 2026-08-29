package com.simibubi.create.gametest.client;

import java.util.Objects;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a water wheel, drives it with a creative motor and photographs the result.
 * <p>
 * The point of a client gametest is that the whole client is real: a world is created through the
 * menus, the chunks are rendered, and the screenshot at the end is the frame the game actually drew.
 * So this covers rather more than the two blocks it places - world creation, Create's registration,
 * the kinetic network forming across a block boundary, and Flywheel drawing the result.
 */
public class WaterWheelClientGameTest implements FabricClientGameTest {

	/**
	 * The superflat preset the test world builder uses puts grass at y=-61, so the first free block
	 * above the ground is y=-60.
	 */
	private static final BlockPos WHEEL = new BlockPos(3, -60, 0);

	/** The motor sits on the wheel's north face, where the wheel's shaft comes out. */
	private static final BlockPos MOTOR = WHEEL.north();

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.create()) {
			singleplayer.getClientLevel()
				.waitForChunksRender();

			TestServerContext server = singleplayer.getServer();

			// facing is the axis the shaft runs along, so both blocks face along z and meet.
			server.runCommand("setblock %d %d %d create:water_wheel[facing=north]".formatted(WHEEL.getX(),
				WHEEL.getY(), WHEEL.getZ()));
			server.runCommand("setblock %d %d %d create:creative_motor[facing=south]".formatted(MOTOR.getX(),
				MOTOR.getY(), MOTOR.getZ()));

			// Stand off the south-east corner looking north-west, so the shot catches the face of the
			// wheel and the motor beside it rather than one hiding behind the other.
			server.runCommand("tp @a %.1f %d %.1f 141 10".formatted(WHEEL.getX() + 4.5, WHEEL.getY(),
				WHEEL.getZ() + 4.5));

			// Long enough for the network to form and the wheel to spin up to the motor's speed.
			context.waitTicks(40);

			server.runOnServer(minecraftServer -> assertDriven(minecraftServer.overworld()));

			context.takeScreenshot("water_wheel_driven_by_creative_motor");
		}
	}

	private static void assertDriven(ServerLevel level) {
		BlockState wheel = level.getBlockState(WHEEL);
		BlockState motor = level.getBlockState(MOTOR);

		if (!AllBlocks.WATER_WHEEL.has(wheel))
			throw new AssertionError("Expected a water wheel at " + WHEEL + ", found " + wheel);
		if (!AllBlocks.CREATIVE_MOTOR.has(motor))
			throw new AssertionError("Expected a creative motor at " + MOTOR + ", found " + motor);

		KineticBlockEntity wheelBe = kineticAt(level, WHEEL);
		KineticBlockEntity motorBe = kineticAt(level, MOTOR);

		// The wheel generates nothing without water, so any speed it has comes from the motor.
		if (wheelBe.getSpeed() == 0)
			throw new AssertionError("The water wheel is not turning, so the motor did not reach it");

		if (!Objects.equals(wheelBe.network, motorBe.network))
			throw new AssertionError("The water wheel and the motor are on separate kinetic networks: "
				+ wheelBe.network + " and " + motorBe.network);

		// The two are shaft to shaft, so no gearing sits between them to change the rate. Which way
		// round the sign comes out is the propagator's business, so only the rate is checked.
		if (Math.abs(wheelBe.getSpeed()) != Math.abs(motorBe.getSpeed()))
			throw new AssertionError("Expected the wheel to turn at the motor's speed, but the wheel is at "
				+ wheelBe.getSpeed() + " and the motor at " + motorBe.getSpeed());
	}

	private static KineticBlockEntity kineticAt(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);

		if (!(be instanceof KineticBlockEntity kinetic))
			throw new AssertionError("Expected a kinetic block entity at " + pos + ", found " + be);

		return kinetic;
	}

}
