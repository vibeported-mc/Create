package com.simibubi.create.gametest.client.trains;

import com.simibubi.create.gametest.client.gui.ScreenTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.trains.station.AssemblyScreen;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The train station's two screens, which are the same block answering in two different states.
 * <p>
 * A station is the one thing here that cannot simply be put down and clicked: it only has a screen once
 * it belongs to a stretch of track, and it is given that as it is placed. So both tests lay a run of
 * rails, click them with the station in hand to choose which ones it watches, and only then put the
 * station down beside them - the two clicks a player makes.
 * <p>
 * From there one test names the station, and the other asks it for a new train, which turns it over to
 * assembly mode and so opens the other screen the next time it is clicked.
 * <p>
 * Each test gets a world of its own rather than sharing one. Rails and the stations on them are
 * remembered by the railway rather than only by the blocks, so a stretch of track that has already been
 * claimed once stays claimed even after the blocks are cleared away.
 */
public class StationScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the screen's packet to have crossed and been applied. */
	private static final int SEND_TICKS = 20;

	/** How far the track runs either side of the station. */
	private static final int TRACK_REACH = 4;

	private static final String NAME = "Quarry Line";

	/** Where this test's rails are laid. */
	private BlockPos origin;

	@ClientGameTest(screenshot = false)
	@DisplayName("The name typed onto a train station is the name the station is left with")
	void namesTheStation(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		build(context, singleplayer, server, new BlockPos(60, -58, 60));

		ScreenTesting.rightClickBlock(context, server, station());
		ScreenTesting.waitForScreen(context, StationScreen.class);
		context.takeScreenshot(shot("station_opened"));

		ScreenTesting.click(context, ScreenTesting.widget(context, "nameBox"));
		context.getInput()
			.typeChars(NAME);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("station_named"));

		// A station has no button to accept it; what was typed is sent as the screen is taken away.
		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SEND_TICKS);

		String nameOnStation = server.computeOnServer(minecraftServer -> theStation(minecraftServer).getStation().name);

		assertEquals(NAME, nameOnStation, "The name typed onto the station did not reach it");
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Asking a station for a new train puts it into assembly mode and opens that screen")
	void switchesToAssembly(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		build(context, singleplayer, server, new BlockPos(60, -58, 60));

		ScreenTesting.rightClickBlock(context, server, station());
		ScreenTesting.waitForScreen(context, StationScreen.class);

		// Asking for a new train hands straight over to the other screen rather than closing this one, and
		// tells the block to turn over to assembly as it goes.
		ScreenTesting.click(context, ScreenTesting.widget(context, "newTrainButton"));
		ScreenTesting.waitForScreen(context, AssemblyScreen.class);
		context.waitTicks(SEND_TICKS);
		context.takeScreenshot(shot("station_assembly"));

		boolean assembling = server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockState(station())
			.getValue(StationBlock.ASSEMBLING));

		assertTrue(assembling, "The station did not turn over to assembly mode");

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SEND_TICKS);

		// And which of the two screens it opens from now on is decided by that same state.
		ScreenTesting.rightClickBlock(context, server, station());
		ScreenTesting.waitForScreen(context, AssemblyScreen.class);
		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SEND_TICKS);
	}

	/** A run of rails with a station beside it, put down the way a player puts one down. */
	private void build(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server, BlockPos where) {
		origin = where;

		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, track(), 8);

		// A straight run, so that the station has a stretch of rails to belong to.
		for (int along = -TRACK_REACH; along <= TRACK_REACH; along++)
			server.runCommand("setblock %d %d %d create:track[shape=zo]".formatted(track().getX(), track().getY(),
				track().getZ() + along));

		// The block whose face the station is put against, which is what decides where it lands.
		server.runCommand("setblock %d %d %d minecraft:stone".formatted(anchor().getX(), anchor().getY(),
			anchor().getZ()));

		server.runCommand("item replace entity @a hotbar.0 with create:track_station");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// The first click chooses which rails this station is for. Track lies flat along the bottom of its
		// block, so it is looked down on from one side rather than aimed at head-on, which would pass over
		// it and reach the next piece along.
		ScreenTesting.rightClickAt(context, server, surfaceOf(track()), besideAndAbove(track()), track());
		context.waitTicks(SETTLE_TICKS);

		assertNotNull(server.computeOnServer(minecraftServer -> minecraftServer.getPlayerList()
			.getPlayers()
			.get(0)
			.getMainHandItem()
			.get(com.simibubi.create.AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)),
			"Clicking the rails did not choose them for the station");

		// The second puts the station down, beside the rails rather than on top of them.
		ScreenTesting.rightClickBlock(context, server, anchor());
		context.waitTicks(SETTLE_TICKS);

		assertNotNull(server.computeOnServer(minecraftServer -> theStation(minecraftServer).getStation()),
			"The station was put down but does not belong to any track");

		// Whether the screen opens at all is the client's decision, and it will not make it until it has
		// been told which stretch of track this station belongs to.
		context.waitFor(client -> client.level.getBlockEntity(station()) instanceof StationBlockEntity be
			&& be.getStation() != null);
	}

	private StationBlockEntity theStation(MinecraftServer minecraftServer) {
		BlockEntity be = minecraftServer.overworld()
			.getBlockEntity(station());

		if (!(be instanceof StationBlockEntity stationBe))
			throw new AssertionError("There is no station at " + station() + " but " + be);

		return stationBe;
	}

	/** The middle of the run of track, which is the piece the station is told to watch. */
	private BlockPos track() {
		return origin;
	}

	/** Where the station ends up: beside the rails, in the space beyond the block that was clicked. */
	private BlockPos station() {
		return track().east();
	}

	/** The block whose south face is clicked, which is what decides where the station lands. */
	private BlockPos anchor() {
		return station().north();
	}

	/** Just inside the block, low enough to be on the rails rather than in the air above them. */
	private static Vec3 surfaceOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
	}

	/**
	 * Above and off to the side, looking down.
	 * <p>
	 * To the side across the run rather than along it, so that nothing else on the line stands between the
	 * player and the piece being aimed at.
	 */
	private static Vec3 besideAndAbove(BlockPos pos) {
		return new Vec3(pos.getX() + 2.5, pos.getY() + 3, pos.getZ() + 0.5);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
