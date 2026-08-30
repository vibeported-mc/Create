package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A harvester carried round on a bearing, cutting a ring of wheat and handing what it reaps out through
 * a portable interface.
 * <p>
 * The same build as the one that carries water, with a chest in place of the travelling tank and a
 * harvester on the arm. It turns, the harvester cuts whatever wheat it passes over and drops it into the
 * chest it is glued to, and once a turn the interface comes round to the fixed one and what has been
 * reaped is emptied into the chest below it.
 * <p>
 * As with the water, the interface rides two blocks out from the axle and the fixed one stands a block
 * of air away from where it comes to rest - closer in, the two never meet.
 */
public class PortableHarvestTest {

	/** The height the bearing turns at, with the crop growing at the height of the harvester. */
	private static final int AXLE_Y = -58;

	private static final int BEARING_RPM = 16;

	/**
	 * The ring of wheat: every square around the bearing except the one the harvester rests on.
	 * <p>
	 * The harvester rides one block out, so its circle passes through all eight of them - corners as
	 * well as sides. Further out it would only reach the corners.
	 */
	private static final int[][] CROP_RING =
		{{1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

	private static final int PATIENCE_TICKS = 1200;

	@ClientGameTest(screenshot = false)
	@DisplayName("A harvester carried round on a bearing reaps a ring of wheat into a chest")
	void reapsTheRingIntoTheChest(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		watchTheBearing(context, server);
		clearTheGround(server);
		build(server);

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			// One piece of glue along the whole arm, so the chest, the harvester and the interface all go
			// round together.
			level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(carriedChest(), carriedPort())));

			turn(level, bearingMotor(), BEARING_RPM);
		});

		context.waitTicks(20);
		context.showContainerOverlay(outputChest());
		context.takeScreenshot(shot("harvest_before"));

		assertTrue(contraptionAssembled(server), "The bearing did not assemble anything at all");
		assertTrue(joinedTheContraption(server, harvester()),
			"The harvester did not join the contraption, so the glue did not hold");
		assertTrue(joinedTheContraption(server, carriedPort()),
			"The interface did not join the contraption, so the glue did not reach it");

		int waited = 0;

		while (waited < PATIENCE_TICKS && countIn(server, outputChest(), Items.WHEAT) < CROP_RING.length) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("harvest_after"));
		context.clearOverlays();

		assertEquals(0, ripeCrops(server), "The harvester left some of the ripe wheat standing");
		assertTrue(countIn(server, outputChest(), Items.WHEAT) >= CROP_RING.length,
			"The wheat the harvester reaped did not all reach the chest: "
				+ countIn(server, outputChest(), Items.WHEAT) + " of " + CROP_RING.length + " wheat, and "
				+ countIn(server, outputChest(), Items.WHEAT_SEEDS) + " seeds");
	}

	private void build(TestServerContext server) {
		setBlock(server, bearing(), "create:mechanical_bearing[facing=up]");
		setBlock(server, bearingMotor(), "create:creative_motor[facing=up]");

		// What turns, in a line out from the axle: a chest to reap into, the harvester one block out so
		// that its circle sweeps the whole ring, and the interface one further so that it can reach the
		// fixed one.
		setBlock(server, carriedChest(), "minecraft:chest");
		setBlock(server, harvester(), "create:mechanical_harvester[facing=east,waterlogged=false]");
		setBlock(server, carriedPort(), "create:portable_storage_interface[facing=east]");

		// The one fixed interface, with a chute under it to draw what arrives down into the chest.
		setBlock(server, fixedPort(), "create:portable_storage_interface[facing=north]");
		setBlock(server, new BlockPos(0, AXLE_Y, 4), "create:chute[facing=down,shape=normal]");
		setBlock(server, outputChest(), "minecraft:chest");

		for (int[] square : CROP_RING) {
			setBlock(server, new BlockPos(square[0], AXLE_Y, square[1]), "minecraft:farmland[moisture=7]");
			setBlock(server, new BlockPos(square[0], AXLE_Y + 1, square[1]), "minecraft:wheat[age=7]");
		}
	}

	private BlockPos bearing() {
		return new BlockPos(0, AXLE_Y, 0);
	}

	private BlockPos bearingMotor() {
		return new BlockPos(0, AXLE_Y - 1, 0);
	}

	private BlockPos carriedChest() {
		return new BlockPos(0, AXLE_Y + 1, 0);
	}

	/** One block out from the bearing, so its circle passes over every square of the ring. */
	private BlockPos harvester() {
		return new BlockPos(1, AXLE_Y + 1, 0);
	}

	private BlockPos carriedPort() {
		return new BlockPos(2, AXLE_Y + 1, 0);
	}

	/** Two blocks out from where the turning interface comes to rest, leaving a block of air between. */
	private BlockPos fixedPort() {
		return new BlockPos(0, AXLE_Y + 1, 4);
	}

	private BlockPos outputChest() {
		return new BlockPos(0, AXLE_Y - 1, 4);
	}

	/**
	 * How much of the ring is still ripe.
	 * <p>
	 * A harvester sows again behind itself, so a square it has cut still holds wheat - just wheat that
	 * has only started growing. What tells a cut square from an uncut one is how far grown it is, not
	 * whether anything is there.
	 */
	private int ripeCrops(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			int ripe = 0;

			for (int[] square : CROP_RING) {
				BlockState state = minecraftServer.overworld()
					.getBlockState(new BlockPos(square[0], AXLE_Y + 1, square[1]));

				if (state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == CropBlock.MAX_AGE)
					ripe++;
			}

			return ripe;
		});
	}

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

	private int countIn(TestServerContext server, BlockPos pos, net.minecraft.world.item.Item item) {
		return server.computeOnServer(minecraftServer -> {
			if (!(minecraftServer.overworld()
				.getBlockEntity(pos) instanceof Container container))
				return 0;

			int found = 0;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(item))
					found += stack.getCount();
			}
			return found;
		});
	}

	private void turn(ServerLevel level, BlockPos motor, int rpm) {
		((CreativeMotorBlockEntity) level.getBlockEntity(motor)).generatedSpeed.setValue(rpm);
	}

	private void clearTheGround(TestServerContext server) {
		server.runCommand("fill -4 %d -6 4 %d 6 air".formatted(AXLE_Y - 2, AXLE_Y + 4));
		server.runCommand("kill @e[type=item]");
	}

	private static void watchTheBearing(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 11.0 %d 2.0 facing 0.0 %d 1.0".formatted(AXLE_Y + 5, AXLE_Y + 1));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
