package com.simibubi.create.gametest.client.contraptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.item.ItemHandlerHelpers;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A mechanical mixer turning andesite and an iron nugget into andesite alloy.
 * <p>
 * The mixer stands two above the basin with a gap between them, which is the only arrangement it works
 * in, and is turned by a cogwheel meshing with it - a mixer declares no shaft of its own, the same as the
 * arm and the crafter.
 * <p>
 * The ingredients are dropped in rather than placed: a basin takes items that fall into it, which is what
 * a funnel or a chute above one ends up doing, and what a player does by throwing them.
 */
@SharedWorld
public class MixerTest {

	private static final int SETTLE_TICKS = 10;

	private static final int MIXER_RPM = 64;

	/** Long enough for the mixer to have wound up and worked. */
	private static final int PATIENCE_TICKS = 400;

	@ClientGameTest(screenshot = false)
	@DisplayName("A mixer over a basin turns andesite and an iron nugget into andesite alloy")
	void mixesAndesiteAlloy(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, basin(), 6);

		setBlock(server, basin(), "create:basin");
		setBlock(server, mixer(), "create:mechanical_mixer");
		setBlock(server, cog(), "create:cogwheel[axis=y]");
		setBlock(server, motor(), "create:creative_motor[facing=down]");
		context.waitTicks(SETTLE_TICKS);

		server.runOnServer(minecraftServer -> ((CreativeMotorBlockEntity) minecraftServer.overworld()
			.getBlockEntity(motor())).generatedSpeed.setValue(MIXER_RPM));
		context.waitTicks(SETTLE_TICKS);

		ServerLevel[] world = new ServerLevel[1];
		server.runOnServer(minecraftServer -> world[0] = minecraftServer.overworld());

		context.showOverlay(basin(), () -> {
			if (world[0] == null || !(world[0].getBlockEntity(basin()) instanceof BasinBlockEntity basinBe))
				return "no basin";

			for (int slot = 0; slot < basinBe.getOutputInventory()
				.size(); slot++) {
				ItemStack stack = ItemHandlerHelpers.getStackInSlot(basinBe.getOutputInventory(), slot);

				if (!stack.isEmpty())
					return stack.getCount() + " " + stack.getHoverName()
						.getString();
			}

			return "nothing made yet";
		});

		watchTheBasin(context, server);
		context.takeScreenshot(shot("mixer_before"));

		// Dropped in from above, which is how anything gets into a basin.
		dropIn(server, "minecraft:andesite");
		dropIn(server, "minecraft:iron_nugget");

		int waited = 0;

		while (waited < PATIENCE_TICKS && mixed(server).isEmpty()) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("mixer_after"));
		context.clearOverlays();

		assertEquals(AllItems.ANDESITE_ALLOY.asItem(), mixed(server).getItem(),
			"The basin was not left holding andesite alloy");
	}

	/** What the basin has made, which waits in its own half of the basin until something takes it. */
	private ItemStack mixed(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			BasinBlockEntity basinBe = theBasin(minecraftServer);

			for (int slot = 0; slot < basinBe.getOutputInventory()
				.size(); slot++) {
				ItemStack stack = ItemHandlerHelpers.getStackInSlot(basinBe.getOutputInventory(), slot);

				if (!stack.isEmpty())
					return stack;
			}

			return ItemStack.EMPTY;
		});
	}

	private BasinBlockEntity theBasin(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(basin());

		if (!(be instanceof BasinBlockEntity basinBe))
			throw new AssertionError("There is no basin at " + basin() + " but " + be);

		return basinBe;
	}

	private void dropIn(TestServerContext server, String item) {
		server.runCommand("summon minecraft:item %s %s %s {Item:{id:\"%s\",count:1}}".formatted(
			basin().getX() + 0.5, basin().getY() + 1.2, basin().getZ() + 0.5, item));
	}

	private BlockPos basin() {
		return new BlockPos(60, -58, 60);
	}

	/** Two above the basin, with the gap between them that a mixer needs. */
	private BlockPos mixer() {
		return basin().above(2);
	}

	/** Meshing with the mixer, since a mixer takes no shaft of its own. */
	private BlockPos cog() {
		return mixer().east();
	}

	private BlockPos motor() {
		return cog().above();
	}

	private void watchTheBasin(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		ScreenTesting.lookAt(context, server, Vec3.atCenterOf(basin().above()),
			new Vec3(basin().getX() + 0.5, basin().getY() + 2, basin().getZ() + 5));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

}
