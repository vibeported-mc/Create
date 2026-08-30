package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.gametest.client.gui.ScreenTesting;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * An encased fan blowing through water, which washes whatever the draught reaches.
 * <p>
 * What a fan does to the things in front of it is decided by what its air passes through on the way: fire
 * smokes them, lava blasts them, and water washes them. The catch with water is that a block of it beside
 * a fan simply runs away, so it is put inside a leaf block instead - leaves hold their water and the
 * draught goes straight through them, which is how this is built in play.
 * <p>
 * Below the leaves is a depot with a block of ice on it. Nothing else happens: the fan is left to blow,
 * and the ice should come out packed.
 */
@SharedWorld
public class EncasedFanTest {

	private static final int SETTLE_TICKS = 10;

	private static final int FAN_RPM = 128;

	/** Long enough for the fan to have worked the ice through. */
	private static final int PATIENCE_TICKS = 400;

	@ClientGameTest(screenshot = false)
	@DisplayName("A fan blowing through water washes the ice on the depot below into packed ice")
	void washesWhatTheDraughtReaches(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, depot(), 6);

		setBlock(server, depot(), "create:depot");

		// The water the draught passes through, held in place by the leaves around it.
		setBlock(server, water(), "minecraft:oak_leaves[waterlogged=true,persistent=true]");

		setBlock(server, fan(), "create:encased_fan[facing=down]");
		setBlock(server, motor(), "create:creative_motor[facing=down]");
		context.waitTicks(SETTLE_TICKS);

		server.runOnServer(minecraftServer -> {
			((CreativeMotorBlockEntity) minecraftServer.overworld()
				.getBlockEntity(motor())).generatedSpeed.setValue(FAN_RPM);

			theDepot(minecraftServer).setHeldItem(new ItemStack(Items.ICE));
		});

		ServerLevel[] world = new ServerLevel[1];
		server.runOnServer(minecraftServer -> world[0] = minecraftServer.overworld());

		context.showOverlay(depot(), () -> {
			if (world[0] == null || !(world[0].getBlockEntity(depot()) instanceof DepotBlockEntity depotBe))
				return "no depot";

			ItemStack item = depotBe.getHeldItem();

			return item.isEmpty() ? "empty" : item.getHoverName()
				.getString();
		});

		watchTheFan(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("encased_fan_before"));

		int waited = 0;

		while (waited < PATIENCE_TICKS && !onTheDepot(server).is(Items.PACKED_ICE)) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("encased_fan_after"));
		context.clearOverlays();

		assertEquals(Items.PACKED_ICE, onTheDepot(server).getItem(),
			"The ice under the fan was not washed into packed ice");
	}

	private ItemStack onTheDepot(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> theDepot(minecraftServer).getHeldItem());
	}

	private DepotBlockEntity theDepot(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(depot());

		if (!(be instanceof DepotBlockEntity depotBe))
			throw new AssertionError("There is no depot at " + depot() + " but " + be);

		return depotBe;
	}

	/** What is being washed, directly under the water. */
	private BlockPos depot() {
		return new BlockPos(60, -56, 60);
	}

	private BlockPos water() {
		return depot().above();
	}

	private BlockPos fan() {
		return water().above();
	}

	private BlockPos motor() {
		return fan().above();
	}

	private void watchTheFan(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		ScreenTesting.lookAt(context, server, Vec3.atCenterOf(depot().above()),
			new Vec3(depot().getX() + 0.5, depot().getY() + 1, depot().getZ() + 6));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
