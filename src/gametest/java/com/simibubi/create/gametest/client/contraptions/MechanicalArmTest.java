package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.gametest.client.gui.ScreenTesting;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A mechanical arm carrying an item from one depot to another.
 * <p>
 * An arm is told what to reach for before it is put down rather than after: with the arm in hand, each
 * block clicked becomes one of its points, and clicking one again turns it round from taking to
 * depositing. So the test clicks the near depot once and the far depot twice, and only then puts the arm
 * down between them - which is the moment the arm asks the client what was chosen.
 * <p>
 * After that nothing is driven by hand. A cogwheel beside the arm turns it - an arm takes no shaft of its
 * own, so this is the only way in - an item is laid on the near depot, and the arm should pick it up and
 * set it down on the far one of its own accord.
 */
public class MechanicalArmTest {

	private static final int SETTLE_TICKS = 10;

	/** Slow enough to watch the arm swing across and back. */
	private static final int ARM_RPM = 32;

	/** Generous: an arm waits, swings, waits again, and only then lets go. */
	private static final int PATIENCE_TICKS = 800;

	@ClientGameTest(screenshot = false)
	@DisplayName("A mechanical arm moves an item from the depot it takes from to the one it fills")
	void movesAnItemBetweenDepots(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, arm(), 8);

		// An arm takes no shaft of its own, so it is turned by a cogwheel meshing with it from the side,
		// and that cogwheel is the one the motor drives from below.
		setBlock(server, cog(), "create:cogwheel[axis=y]");
		setBlock(server, motor(), "create:creative_motor[facing=up]");
		setBlock(server, takesFrom(), "create:depot");
		setBlock(server, fills(), "create:depot");

		// What the arm is put down against, which is what decides where it lands.
		setBlock(server, anchor(), "minecraft:stone");

		server.runCommand("item replace entity @a hotbar.0 with create:mechanical_arm");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// One click on a depot makes it a point the arm takes from; a second turns it round into one the
		// arm fills. So the near depot is clicked once and the far one twice.
		ScreenTesting.rightClickBlock(context, server, takesFrom());
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickBlock(context, server, fills());
		context.waitTicks(SETTLE_TICKS);
		ScreenTesting.rightClick(context);
		context.waitTicks(SETTLE_TICKS);

		// Putting the arm down is what sends the choice across.
		ScreenTesting.rightClickBlock(context, server, anchor());
		context.waitTicks(SETTLE_TICKS);

		boolean armIsThere = server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockEntity(arm()) instanceof ArmBlockEntity);

		assertTrue(armIsThere, "The arm was not put down where it was meant to be");

		ServerLevel[] world = new ServerLevel[1];

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			world[0] = level;

			((CreativeMotorBlockEntity) level.getBlockEntity(motor())).generatedSpeed.setValue(ARM_RPM);
			depotAt(minecraftServer, takesFrom()).setHeldItem(new ItemStack(Items.COBBLESTONE));
		});

		context.waitTicks(SETTLE_TICKS);

		float armSpeed = server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(arm());

			return be instanceof ArmBlockEntity armBe ? armBe.getSpeed() : 0f;
		});

		assertTrue(armSpeed != 0, "The cogwheel beside it is not turning the arm");

		showDepot(context, world, takesFrom(), "from");
		showDepot(context, world, fills(), "to");

		watchTheArm(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("mechanical_arm_before"));

		int waited = 0;

		while (waited < PATIENCE_TICKS && held(server, fills()).isEmpty()) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("mechanical_arm_after"));
		context.clearOverlays();

		assertEquals(Items.COBBLESTONE, held(server, fills()).getItem(),
			"The arm did not set the item down on the depot it was told to fill");
		assertTrue(held(server, takesFrom()).isEmpty(),
			"The arm left the item on the depot it was told to take from as well");
	}

	/** Stands back far enough to see both depots and the arm between them. */
	private void watchTheArm(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		ScreenTesting.lookAt(context, server, Vec3.atCenterOf(arm()),
			new Vec3(arm().getX() + 0.5, arm().getY() + 2, arm().getZ() + 5.5));
	}

	/** Where the arm ends up: on the ground between the two depots. */
	private BlockPos arm() {
		return new BlockPos(60, -58, 60);
	}

	/** The block the arm is put down against, one step behind where it lands. */
	private BlockPos anchor() {
		return arm().north();
	}

	/** The cogwheel meshing with the arm, which is what actually turns it. */
	private BlockPos cog() {
		return arm().east();
	}

	private BlockPos motor() {
		return cog().below();
	}

	private BlockPos takesFrom() {
		return arm().west(2);
	}

	private BlockPos fills() {
		return arm().east(2);
	}

	/** What a depot is holding, on the server. */
	private ItemStack held(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> depotAt(minecraftServer, pos).getHeldItem());
	}

	private static DepotBlockEntity depotAt(MinecraftServer minecraftServer, BlockPos pos) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(pos);

		if (!(be instanceof DepotBlockEntity depot))
			throw new AssertionError("There is no depot at " + pos + " but " + be);

		return depot;
	}

	/** Notes what a depot is holding beside it, so a run can be followed by eye. */
	private void showDepot(ClientGameTestContext context, ServerLevel[] world, BlockPos pos, String label) {
		context.showOverlay(pos, () -> {
			if (world[0] == null || !(world[0].getBlockEntity(pos) instanceof DepotBlockEntity depot))
				return label + ": no depot";

			ItemStack item = depot.getHeldItem();

			return label + ": " + (item.isEmpty() ? "empty" : item.getCount() + " " + item.getHoverName()
				.getString());
		});
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
