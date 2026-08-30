package com.simibubi.create.gametest.client.trains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationScreen;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.gametest.client.gui.ScreenTesting;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * A railway laid, a train built on it, and a schedule run between two stations - all of it clicked
 * together the way a player does it rather than conjured with commands.
 * <p>
 * Track in particular cannot be conjured. There is no curve to place: only straights and diagonals exist
 * as blocks, and every real turn is a curve the placement code works out from where the player stood and
 * which way they were looking. Clicking it together is the only way to get one, which is why this test
 * builds the circuit the long way round.
 * <p>
 * The shape is a running track: two long straights joined at each end by a half turn, and each half turn
 * is two quarter turns with a short piece between them - a single sweep through half a circle is more
 * than the placement will agree to.
 * <p>
 * The ground is the world's own: flat grass at one height across the whole circuit, so nothing needs
 * levelling first.
 */
@SharedWorld
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrainCircuitTest {

	private static final int SETTLE_TICKS = 10;

	/** The height the track sits at; the grass it rests on is the block below. */
	private static final int RAIL_Y = -60;

	/** How far a quarter turn reaches, across and along, which is what makes it wide enough to drive. */
	private static final int CORNER = 8;

	/** The circuit, side by side: the way each runs and how far it goes before it turns. */
	private static final Direction[] SIDES =
		{ Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST };
	private static final int[] LENGTHS = { 20, 4, 20, 4 };

	/** Where the corners ended up, kept from the laying so the phases after can check them. */
	private final List<BlockPos> turns = new ArrayList<>();

	@ClientGameTest(screenshot = false)
	@Order(1)
	@DisplayName("A closed circuit of track is laid by clicking, half turns and all")
	void laysACircuitOfTrack(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		server.runCommand("item replace entity @a hotbar.0 with create:track 64");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		// The first piece goes down like any block. Which way it lies follows the way the player faces, so
		// it is clicked at from one side rather than from straight overhead.
		placeTrackFacing(context, server, start(), SIDES[0]);

		BlockPos cursor = start();
		turns.clear();

		for (int side = 0; side < SIDES.length; side++) {
			Direction along = SIDES[side];
			Direction next = SIDES[(side + 1) % SIDES.length];

			// Along the same line, picking the far end and clicking on runs the track out straight.
			selectEnd(context, server, cursor, along);

			BlockPos turn = cursor.relative(along, LENGTHS[side]);
			placeTrackFacing(context, server, turn, along);
			turns.add(turn);

			// Across it, the placement works the quarter turn out for itself.
			selectEnd(context, server, turn, along);

			BlockPos landing = turn.relative(along, CORNER)
				.relative(next, CORNER);

			// The last turn comes back round onto the piece it all started from, closing the ring.
			if (side == SIDES.length - 1)
				connectToExisting(context, server, start(), next);
			else
				placeTrackFacing(context, server, landing, next);

			cursor = landing;
		}

		watchTheCircuit(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("circuit_laid"));

		for (BlockPos turn : turns)
			assertTrue(hasCurveAt(server, turn), "The corner at " + turn
				+ " was not turned into a curve; the track there is " + describe(server, turn) + " | map " + map(server));

		assertEquals(1, graphCount(server),
			"The circuit did not come out as one railway but as " + graphCount(server));
	}

	@ClientGameTest(screenshot = false)
	@Order(2)
	@DisplayName("A station is put on each straight and given its name")
	void placesTwoStations(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(1, graphCount(server),
			"There is no railway to put stations on - did the circuit fail to be laid?");

		placeStation(context, server, STATION_A, trackFor(STATION_A), Direction.SOUTH);
		placeStation(context, server, STATION_B, trackFor(STATION_B), Direction.NORTH);

		nameStation(context, server, STATION_A, "Station A");
		nameStation(context, server, STATION_B, "Station B");

		watchTheCircuit(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("stations_placed"));

		assertEquals("Station A", stationName(server, STATION_A), "The first station kept the wrong name");
		assertEquals("Station B", stationName(server, STATION_B), "The second station kept the wrong name");
	}

	/**
	 * Puts a station beside the track, the way one is put down in play.
	 * <p>
	 * Two clicks: the rail itself, which is what the station will watch and which way along it a train is
	 * built, and then the ground a couple of blocks to the side, which is where the station lands.
	 */
	private void placeStation(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		BlockPos track, Direction along) {
		server.runCommand("item replace entity @a hotbar.0 with create:track_station");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickAt(context, server, surfaceOf(track), standingBack(track, along), track);
		context.waitTicks(SETTLE_TICKS);

		BlockPos ground = pos.below();

		ScreenTesting.rightClickAt(context, server, topOf(ground), standingBack(ground, along), ground);
		context.waitTicks(SETTLE_TICKS);
	}

	/** Opens the station and types its name in, which is the only way a station gets one. */
	private void nameStation(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		String name) {
		// The screen only opens once the client has been told which rails this station belongs to.
		context.waitFor(client -> client.level.getBlockEntity(pos) instanceof StationBlockEntity be
			&& be.getStation() != null);

		ScreenTesting.rightClickBlock(context, server, pos);
		ScreenTesting.waitForScreen(context, StationScreen.class);

		ScreenTesting.click(context, ScreenTesting.widget(context, "nameBox"));
		context.getInput()
			.typeChars(name);
		context.waitTicks(SETTLE_TICKS);

		// A station has no button to accept it; what was typed is sent as the screen is taken away.
		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);
	}

	private String stationName(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> {
			var be = minecraftServer.overworld()
				.getBlockEntity(pos);

			if (!(be instanceof StationBlockEntity stationBe) || stationBe.getStation() == null)
				throw new AssertionError("There is no station on the railway at " + pos + " but " + be);

			return stationBe.getStation().name;
		});
	}

	/** The piece of rail each station watches: the middle of its own straight. */
	private BlockPos trackFor(BlockPos station) {
		return station.equals(STATION_A) ? new BlockPos(52, RAIL_Y, 62) : new BlockPos(72, RAIL_Y, 62);
	}

	/** Beside each straight, a couple of blocks clear of the rails. */
	private static final BlockPos STATION_A = new BlockPos(50, RAIL_Y, 62);
	private static final BlockPos STATION_B = new BlockPos(74, RAIL_Y, 62);

	/** The one piece everything else hangs off, and the point the ring closes back onto. */
	private BlockPos start() {
		return new BlockPos(52, RAIL_Y, 52);
	}

	/**
	 * Picks the end of a piece of track to build on from.
	 * <p>
	 * Which end is chosen follows the way the player looks at it, so this stands back on the near side and
	 * looks along the track towards the end it wants.
	 */
	private void selectEnd(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		Direction end) {
		ScreenTesting.rightClickAt(context, server, surfaceOf(pos), standingBack(pos, end), pos);
		context.waitTicks(SETTLE_TICKS);
	}

	/** Clicks the ground where a piece belongs, with the player facing the way it should run. */
	private void placeTrackFacing(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		Direction facing) {
		BlockPos ground = pos.below();

		ScreenTesting.rightClickAt(context, server, topOf(ground), standingBack(ground, facing), ground);
		context.waitTicks(SETTLE_TICKS);
	}

	/** The same, onto track already there, which is how the ring is closed rather than extended. */
	private void connectToExisting(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		Direction facing) {
		ScreenTesting.rightClickAt(context, server, surfaceOf(pos), standingBack(pos, facing), pos);
		context.waitTicks(SETTLE_TICKS);
	}

	/** Above and back along the given way, so the player both looks that way and looks down on it. */
	private static Vec3 standingBack(BlockPos pos, Direction facing) {
		return new Vec3(pos.getX() + 0.5 - facing.getStepX() * 3, pos.getY() + 2,
			pos.getZ() + 0.5 - facing.getStepZ() * 3);
	}

	private boolean hasCurveAt(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockEntity(pos) instanceof TrackBlockEntity track && !track.getConnections()
				.isEmpty());
	}

	private String describe(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getBlockState(pos)
			.toString());
	}

	/** A picture of what track ended up where, so a circuit can be read at a glance. */
	private String map(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			StringBuilder report = new StringBuilder();

			for (int z = 42; z <= 84; z += 2) {
				for (int x = 50; x <= 76; x += 2)
					report.append(minecraftServer.overworld()
						.getBlockEntity(new BlockPos(x, RAIL_Y, z)) instanceof TrackBlockEntity ? 'C'
							: minecraftServer.overworld()
								.getBlockState(new BlockPos(x, RAIL_Y, z))
								.getBlock() == com.simibubi.create.AllBlocks.TRACK.get() ? '#' : '.');

				report.append('/');
			}

			return report.toString();
		});
	}

	/** How many separate railways the world thinks it has, which for one closed ring should be one. */
	private int graphCount(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> Create.RAILWAYS.trackNetworks.size());
	}

	/** High up and off one end, far enough back that the whole circuit is in frame. */
	private void watchTheCircuit(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		ScreenTesting.lookAt(context, server, new Vec3(62, RAIL_Y, 62), new Vec3(62, RAIL_Y + 26, 96));
	}

	/** Just inside the block, low enough to be on the rails rather than in the air above them. */
	private static Vec3 surfaceOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
	}

	private static Vec3 topOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
