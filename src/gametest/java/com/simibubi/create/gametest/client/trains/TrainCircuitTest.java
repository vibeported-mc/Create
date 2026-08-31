package com.simibubi.create.gametest.client.trains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.AssemblyScreen;
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
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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
	private static final int[] LENGTHS = { 20, 20, 20, 20 };

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
	@DisplayName("Three stations are put on the circuit and given their names")
	void placesThreeStations(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(1, graphCount(server),
			"There is no railway to put stations on - did the circuit fail to be laid?");

		// Two on the near straight and one across the circuit. The parking station is where the train is
		// built and where it comes home to, so that a lap of the circuit is a thing of its own and has
		// nothing to do with loading or unloading.
		// Each is named where it stands, before moving on to the next: the player is already there, and
		// going back round afterwards is another journey for nothing.
		placeAndName(context, server, PARKING, PARKING_NAME);
		placeAndName(context, server, LOADING, LOADING_NAME);
		placeAndName(context, server, UNLOADING, UNLOADING_NAME);

		watchTheCircuit(context, server);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("stations_placed"));

		assertEquals(PARKING_NAME, stationName(server, PARKING), "The parking station kept the wrong name");
		assertEquals(LOADING_NAME, stationName(server, LOADING), "The loading station kept the wrong name");
		assertEquals(UNLOADING_NAME, stationName(server, UNLOADING),
			"The unloading station kept the wrong name");
	}

	@ClientGameTest(screenshot = false)
	@Order(3)
	@DisplayName("A train is built on the rails at the parking station and assembled from its screen")
	void buildsATrainAtTheParkingStation(ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(PARKING_NAME, stationName(server, PARKING),
			"There is no named station to build a train at - did the station phase fail?");

		enterAssemblyMode(context, server);

		// A bogey appears on the rail wherever the marked stretch is clicked. One is enough for a carriage,
		// and every bogey put down needs a body of its own hung off it.
		server.runCommand("item replace entity @a hotbar.0 with create:railway_casing");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		clickTrack(context, server, bogeyTrack(0));

		// A station only counts up what is standing on its rails on a lazy tick, so what it says about
		// them is a good deal older than the click that put them there.
		context.waitTicks(SETTLE_TICKS * 4);

		assertTrue(bogeyOffsets(server).startsWith("[0, -1,"),
			"Clicking the marked rail did not put bogeys down; the station has " + bogeyOffsets(server)
				+ " | " + assemblyState(server));

		// The carriage itself is set out rather than clicked together: the fittings have to sit exactly
		// where they belong, and a bogey answers a click of its own instead of taking a block.
		buildCarriage(server);
		context.waitTicks(SETTLE_TICKS);

		// Glue holds the carriage together. A bogey only grips along its own length, so anything stacked on
		// top of it - which is the whole body - is not part of the train until it is glued on. The box takes
		// in the floor and everything standing on it.
		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			level.addFreshEntity(new SuperGlueEntity(level,
				SuperGlueEntity.span(floor(-1, -1), floor(1, 1).above())));
		});
		context.waitTicks(SETTLE_TICKS);

		assemble(context, server);

		assertEquals(1, trainCount(server), "The station did not assemble a train; its bogeys were at "
			+ bogeyOffsets(server) + ", it could build over " + assemblyLength(server)
			+ " blocks, and it last complained of " + lastComplaint(server) + " | " + assemblyState(server));

		// Everything the carriage was built out of has to have come with it. Whatever the glue missed would
		// have been left standing on the rails instead, so this is what shows it took the whole floor.
		for (String fitting : new String[] { "create:controls", "create:red_seat", "minecraft:chest",
			"create:fluid_tank", "create:portable_storage_interface", "create:portable_fluid_interface" })
			assertTrue(carriageHolds(server, fitting), "The train was assembled without its " + fitting
				+ ", so the glue did not take the whole floor with it. It carries " + carriageContents(server)
				+ " and what is still standing on the rails is " + leftBehind(server));

		watchTheTrain(context, server);
		context.takeScreenshot(shot("train_assembled"));
	}

	@ClientGameTest(screenshot = false)
	@Order(4)
	@DisplayName("The train is ridden a full circuit by hand and parked back at the parking station")
	void ridesTheTrainRoundTheCircuit(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(1, trainCount(server), "There is no train to ride - did the building phase fail?");

		// A lap and nothing else: away down the far side of the circuit, past the other two stations
		// without stopping at either, and home to the one it set off from.
		driveTo(context, server, PARKING_NAME, true, "circuit");

		assertEquals(theTrainsName(server), trainWaitingAt(server, PARKING),
			"The train did not end up parked at the station it set off from");
	}

	@ClientGameTest(screenshot = false)
	@Order(5)
	@DisplayName("A loading building and an unloading building are put up, both standing idle")
	void buildsTheLoadingAndUnloadingStations(ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(1, trainCount(server), "There is no train to load - did the building phase fail?");

		build(server, loadingSide(), true);
		build(server, unloadingSide(), false);
		context.waitTicks(SETTLE_TICKS);

		// Both stand still to begin with. A belt running before the train is there feeds an interface with
		// nothing on the other side of it, and whatever it sends that way is simply gone.
		stopTheBelt(server, loadingSide());
		stopTheBelt(server, unloadingSide());

		watchTheBuilding(context, server, loadingSide());
		context.takeScreenshot(shot("loading_station"));

		watchTheBuilding(context, server, unloadingSide());
		context.takeScreenshot(shot("unloading_station"));

		for (BlockPos where : new BlockPos[] { loadingSide(), unloadingSide() }) {
			assertEquals("create:portable_storage_interface", blockAt(server, where),
				"The building at " + where + " has no interface for the train to draw up against");

			// A funnel that finds itself standing on a belt becomes a belt funnel, which is the whole point
			// of putting it there, so either name means it is where it should be.
			assertTrue(blockAt(server, stationFunnel(where)).endsWith("funnel"),
				"The building at " + where + " has no funnel on the belt beside its interface, but "
					+ blockAt(server, stationFunnel(where)));
			assertTrue(blockAt(server, farFunnel(where)).endsWith("funnel"),
				"The building at " + where + " has no funnel on the belt beside its chest, but "
					+ blockAt(server, farFunnel(where)));
		}

		// Nothing is asked here about where the load has got to. The funnel at the chest empties it onto
		// the belt the moment it is put there, stopped belt or not, so it is staged along the run rather
		// than sitting in the chest; whether it reaches the train is what the next phase is for.
	}

	@ClientGameTest(screenshot = false)
	@Order(6)
	@DisplayName("The train draws in at the loading station and takes the goods aboard")
	void loadsTheTrainAtTheLoadingStation(ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		driveTo(context, server, LOADING_NAME, false, "to_the_loading_station");

		assertEquals(theTrainsName(server), trainWaitingAt(server, LOADING),
			"The train did not draw in at the loading station");

		// Off the train and stood back, so that the whole of it and the whole of the building it has drawn
		// up against are in one view before anything moves.
		stepOffTheTrain(context, server);
		labelTheStores(context, server, loadingSide());
		watchTheWholeSetup(context, server, loadingSide());
		context.takeScreenshot(shot("train_at_the_loading_station"));

		// Only now is the belt set going. Until the train drew in there was nothing on the far side of the
		// interface, and a belt running into one that is not coupled up loses what it carries.
		driveTheBelt(server, loadingSide(), true);
		context.waitTicks(SETTLE_TICKS);

		// And only then the goods. Put in any earlier they would be drawn along a belt into an interface
		// with nothing on the other side of it, which is the same as throwing them away.
		fillTheSourceChest(server);
		System.out.println("[train] put " + CARGO + " into the chest at " + sourceChest() + ", which now has "
			+ inTheChestOf(server, loadingSide()));

		int waited = 0;

		while (waited < LOADING_TIMEOUT && cargo(server) < CARGO) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("train_loaded"));

		assertEquals(CARGO, cargo(server), "The train did not take its load aboard; " + whereTheGoodsWent(server));
	}

	@ClientGameTest(screenshot = false)
	@Order(7)
	@DisplayName("The train carries the goods across the circuit and is emptied at the unloading station")
	void unloadsTheTrainAtTheUnloadingStation(ClientGameTestContext context,
		TestSingleplayerContext singleplayer, TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(CARGO, cargo(server),
			"The train is not carrying anything to unload - did the loading phase fail?");

		driveTo(context, server, UNLOADING_NAME, false, "to_the_unloading_station");

		assertEquals(theTrainsName(server), trainWaitingAt(server, UNLOADING),
			"The train did not draw in at the unloading station");

		stepOffTheTrain(context, server);
		labelTheStores(context, server, unloadingSide());
		watchTheWholeSetup(context, server, unloadingSide());
		context.takeScreenshot(shot("train_at_the_unloading_station"));

		driveTheBelt(server, unloadingSide(), false);

		int waited = 0;

		while (waited < LOADING_TIMEOUT && inTheChestOf(server, unloadingSide()) < CARGO) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("train_unloaded"));

		assertEquals(CARGO, inTheChestOf(server, unloadingSide()),
			"The unloading building did not get the load off the train; it still holds " + cargo(server));
	}

	/** Few enough to count exactly, and enough to watch travel. */
	private static final int CARGO = 10;

	/**
	 * Takes the controls and drives the train to a named station, drawing it in when it offers.
	 * <p>
	 * Nothing here is asked of the server: the keys are held down for real and the client works out what to
	 * send from them, and every step of the run is read off what the train tells its driver.
	 *
	 * @param aLapFirst whether the train has to have got well away before a station's offer is taken up,
	 *                  which is the difference between a lap of the circuit and a run down the straight
	 */
	private void driveTo(ClientGameTestContext context, TestServerContext server, String station,
		boolean aLapFirst, String pictures) {
		takeTheControls(context, server);

		Vec3 setOffFrom = trainIsAt(context);

		context.getInput()
			.holdKey(options -> options.keyUp);

		// Which way it pulls away is the train's business rather than this test's, so the camera is turned
		// to wherever it has actually started going and the driver watches the track ahead.
		context.waitFor(client -> trainAt(client).distanceTo(setOffFrom) > 2, DRIVE_TIMEOUT);
		lookAlong(context, wayItIsGoing(context, setOffFrom));
		context.takeScreenshot(shot(pictures + "_under_way"));

		List<String> heard = new ArrayList<>();
		Vec3 wasAt = setOffFrom;
		double furthest = 0;
		boolean beenAway = !aLapFirst;
		boolean seenItTurning = !aLapFirst;
		boolean askedToStop = false;
		boolean arrived = false;
		String wrongStation = null;

		for (int waited = 0; waited < DRIVE_TIMEOUT && !arrived; waited += WATCH_TICKS) {
			context.waitTicks(WATCH_TICKS);

			String saying = context.computeOnClient(TrainCircuitTest::prompt);
			double away = context.computeOnClient(client -> trainAt(client).distanceTo(setOffFrom));
			Vec3 nowAt = trainIsAt(context);
			Vec3 moved = nowAt.subtract(wasAt);
			wasAt = nowAt;

			furthest = Math.max(furthest, away);

			// Somewhere on a corner, from over the driver's shoulder. Everything a contraption is made of
			// has to turn with it, and a block drawn by a renderer of its own - the chest here - is the one
			// that shows when something does not; on a straight it would look right either way. The view
			// has to be pulled back out of the carriage to see any of this, since the driver is inside it.
			if (beenAway && !seenItTurning && onACorner(moved)) {
				watchOverTheShoulder(context, true);
				context.takeScreenshot(shot(pictures + "_on_a_corner"));
				watchOverTheShoulder(context, false);
				seenItTurning = true;
			}

			if (!saying.isEmpty() && !heard.contains(shorten(saying))) {
				heard.add(shorten(saying));

				// Put on the console as it is heard, so that a run which never finishes still says what
				// the train was telling its driver on the way.
				System.out.println("[train] " + shorten(saying));
			}

			if (!beenAway && away > FAR_SIDE) {
				beenAway = true;
				context.takeScreenshot(shot(pictures + "_far_side"));
			}

			// Nothing here measures how close the station is: the train says when it is near enough to hand
			// the rest of the run to, and it names the one it means, so the others are driven past. Each
			// station has a straight of the square to itself, which is what gives the key time to reach the
			// server while the one that was offered is still the nearest.
			if (beenAway && !askedToStop && saying.contains("approach " + station)) {
				context.takeScreenshot(shot(pictures + "_approaching"));
				context.getInput()
					.holdKey(options -> options.keyJump);
				askedToStop = true;
			}

			arrived = askedToStop && saying.contains("Arrived at " + station);

			// Drawn in somewhere else entirely. Worth stopping on rather than waiting out the clock, since
			// it says the offer was taken a moment too late and the train was given the next station along.
			if (!arrived && saying.startsWith("Arrived at ")) {
				System.out.println("[train] gave up: wanted " + station + " but " + saying);
				wrongStation = saying;
				break;
			}
		}

		String run = "; it got " + Math.round(furthest) + " blocks off and heard " + heard;

		if (!arrived) {
			System.out.println("[train] never drew in at " + station + run);
			context.takeScreenshot(shot(pictures + "_gave_up"));
		}

		context.getInput()
			.releaseKey(options -> options.keyUp);
		context.getInput()
			.releaseKey(options -> options.keyJump);
		context.waitTicks(SETTLE_TICKS);

		assertNull(wrongStation, "The train was sent to " + station + " and drew in somewhere else: "
			+ wrongStation + run);
		assertTrue(beenAway, "The train never got away from where it started" + run);
		assertTrue(askedToStop, "The train never offered to draw in at " + station + run);
		assertTrue(arrived, "The train was asked to draw in at " + station + " but never got there" + run);
	}

	/**
	 * Gets the player aboard and at the controls.
	 * <p>
	 * Sitting down and taking hold are both clicks on the train itself, which is an entity rather than
	 * blocks in the world, so what the crosshair is on has to be looked for rather than worked out - and
	 * where the train is at the time is asked of the train, since it is not where it was built.
	 */
	private void takeTheControls(ClientGameTestContext context, TestServerContext server) {
		if (driving(context))
			return;

		standBeside(context, server);

		assertTrue(clickOnTrain(context, "create:red_seat", middleOf(whereOnTheTrain(server, "create:red_seat"))),
			"Could not find the seat to sit on");
		assertTrue(riding(context), "Clicking the seat did not sit the player on the train");

		assertTrue(clickOnTrain(context, "create:controls", middleOf(whereOnTheTrain(server, "create:controls"))),
			"Could not find the controls from the seat; the crosshair was on "
				+ whatIsUnderTheCrosshair(context));
		assertTrue(driving(context), "Clicking the controls did not hand the player the train");
	}

	/** Where one of the train's own blocks has got to, which is not where it was built. */
	private BlockPos whereOnTheTrain(TestServerContext server, String wanted) {
		return server.computeOnServer(minecraftServer -> {
			for (Train train : Create.RAILWAYS.trains.values())
				for (Carriage carriage : train.carriages) {
					CarriageContraptionEntity entity = carriage.anyAvailableEntity();

					if (entity == null)
						continue;

					for (Map.Entry<BlockPos, StructureBlockInfo> block : entity.getContraption()
						.getBlocks()
						.entrySet())
						if (nameOf(block.getValue()
							.state()).equals(wanted))
							return BlockPos
								.containing(entity.toGlobalVector(Vec3.atCenterOf(block.getKey()), 1));
				}

			return BlockPos.ZERO;
		});
	}

	/**
	 * Shows what a building's chest and the train's hold are carrying, beside them in the world.
	 * <p>
	 * A picture of a belt says nothing about what is in the chest at the end of it, and where the load has
	 * got to is the whole point of these two phases, so both ends are written up where they can be seen.
	 */
	private void labelTheStores(ClientGameTestContext context, TestServerContext server, BlockPos building) {
		context.showContainerOverlay(chestOf(building));
		context.showOverlay(building, () -> "train: " + cargo(server));
	}

	/** Stands well back from a building, with the train it serves in the same view. */
	private void watchTheWholeSetup(ClientGameTestContext context, TestServerContext server,
		BlockPos building) {
		Direction out = towardsTheTrain(building).getOpposite();
		Direction side = driveSideOf(building);

		// The middle of the whole thing: the train on one side of the interface, the belt and its chest
		// running away on the other. Watched from off to one side and a little above, close enough to see
		// what is on the belt.
		Vec3 middle = middleOf(building.relative(out, 2));

		ScreenTesting.lookAt(context, server, middle,
			middle.add(side.getStepX() * 10, 7, side.getStepZ() * 10));
	}

	/** How much of the load a building's chest is holding. */
	private int inTheChestOf(TestServerContext server, BlockPos building) {
		return server.computeOnServer(minecraftServer -> {
			int found = 0;

			if (minecraftServer.overworld()
				.getBlockEntity(chestOf(building)) instanceof Container chest)
				for (int slot = 0; slot < chest.getContainerSize(); slot++)
					found += chest.getItem(slot)
						.getCount();

			return found;
		});
	}

	/** Leaves a building's belt standing still, which is how it is put up. */
	private void stopTheBelt(TestServerContext server, BlockPos where) {
		BlockPos motor = beltEnd(where).relative(driveSideOf(where)
			.getOpposite());

		server.runOnServer(minecraftServer -> {
			if (minecraftServer.overworld()
				.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
				be.generatedSpeed.setValue(0);
		});
	}

	/** Whichever way the train has actually pulled away, which is where the driver ought to be looking. */
	private Direction wayItIsGoing(ClientGameTestContext context, Vec3 from) {
		Vec3 gone = trainIsAt(context).subtract(from);

		return Direction.getApproximateNearest(gone.x, 0, gone.z);
	}

	/** For when the crosshair did not find what it was after, so the failure can say what it did find. */
	private String whatIsUnderTheCrosshair(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			AbstractContraptionEntity train = trainEntity(client);

			if (train == null)
				return "nothing - there is no train here";

			Vec3 origin = client.player.getEyePosition();
			Vec3 target = origin.add(client.player.getLookAngle()
				.scale(client.player.blockInteractionRange()));
			BlockHitResult hit = ContraptionHandlerClient.rayTraceContraption(origin, target, train);

			if (hit == null)
				return "none of the train, from " + origin + " looking " + client.player.getLookAngle();

			StructureBlockInfo info = train.getContraption()
				.getBlocks()
				.get(hit.getBlockPos());

			return info == null ? "an empty square of the train" : nameOf(info.state());
		});
	}

	/** Pulls the view back out of the carriage, so that the carriage itself can be seen, and puts it back. */
	private void watchOverTheShoulder(ClientGameTestContext context, boolean back) {
		context.runOnClient(client -> client.options
			.setCameraType(back ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON));
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * Whether the train is going somewhere between the compass points, which is to say round a corner.
	 * <p>
	 * Only worth asking of a train that is actually moving, since a heading read off a step of nothing is
	 * whatever rounding says it is.
	 */
	private static boolean onACorner(Vec3 moved) {
		if (moved.horizontalDistance() < 0.05)
			return false;

		double offAxis = Math.floorMod(Math.round(headingOf(moved)), 90);

		return Math.abs(offAxis - 45) < 25;
	}

	/** Which way something moving is headed, in the degrees the game turns a head by. */
	private static float headingOf(Vec3 moved) {
		return (float) (-Mth.atan2(moved.x, moved.z) * 180 / Math.PI);
	}


	/** Long enough for a belt to carry a stack the length of itself and a train to take it aboard. */
	private static final int LOADING_TIMEOUT = 600;

	/** What the loading building has to send, put where a chest at the end of a belt would hold it. */
	private void fillTheSourceChest(TestServerContext server) {
		BlockPos chest = sourceChest();

		server.runCommand("item replace block %d %d %d container.0 with minecraft:cobblestone %d"
			.formatted(chest.getX(), chest.getY(), chest.getZ(), CARGO));
	}

	private BlockPos sourceChest() {
		return chestOf(loadingSide());
	}

	/** How much the train is carrying, read the way a schedule's own conditions read it. */
	private int cargo(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			int found = 0;

			for (Train train : Create.RAILWAYS.trains.values())
				for (Carriage carriage : train.carriages) {
					if (carriage.storage == null)
						continue;

					var items = carriage.storage.getAllItems();

					for (int slot = 0; slot < items.size(); slot++)
						found += ItemUtil.getStack(items, slot)
							.getCount();
				}

			return found;
		});
	}

	/**
	 * What became of what the loading building was given, for when none of it reached the train.
	 * <p>
	 * Goods that miss their way do not vanish - they end up on the ground beside the building - so what is
	 * left in the chest and what is lying about between them say where the run broke down.
	 */
	private String whereTheGoodsWent(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			int left = 0;

			if (level.getBlockEntity(sourceChest()) instanceof Container chest)
				for (int slot = 0; slot < chest.getContainerSize(); slot++)
					left += chest.getItem(slot)
						.getCount();

			int onTheFloor = level
				.getEntitiesOfClass(ItemEntity.class, new AABB(loadingSide()).inflate(BELT_LENGTH + 4))
				.stream()
				.mapToInt(item -> item.getItem()
					.getCount())
				.sum();

			int riding = 0;

			for (BlockPos pos : new BlockPos[] { beltStart(loadingSide()), beltEnd(loadingSide()) })
				if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt)
					for (int offset = 0; offset <= BELT_LENGTH; offset++) {
						TransportedItemStack carried = belt.getInventory()
							.getStackAtOffset(offset);

						if (carried != null)
							riding += carried.stack.getCount();
					}

			String held = "";

			if (level.getBlockEntity(loadingSide()) instanceof Container psi)
				for (int slot = 0; slot < psi.getContainerSize(); slot++)
					if (!psi.getItem(slot)
						.isEmpty())
						held += psi.getItem(slot)
							.getCount() + " ";

			return left + " left in the chest, " + riding + " on the belt, " + onTheFloor
				+ " on the ground and " + (held.isEmpty() ? "none" : held)
				+ " in the building's interface; the train's hold has " + holdSize(minecraftServer)
				+ " slots; it stands at " + loadingSide()
				+ " and the train's is at " + whereTheTrainsInterfaceIs(minecraftServer) + ", "
				+ (docked(minecraftServer) ? "coupled up" : "not coupled up");
		});
	}

	/**
	 * A station building, all of it in one line and all of it at the height of the interface: the chest at
	 * the far end, a funnel, the belt, another funnel, and the interface the train draws up against.
	 * <p>
	 * A funnel's inventory is the block behind its mouth, so each of the two faces away from the thing it
	 * is attached to and towards the belt. Which way goods then move through them is the whole difference
	 * between the two buildings. Where the train is filled, the far funnel empties the chest onto the belt
	 * and the near one takes what arrives and pushes it into the interface; where the train is emptied,
	 * both work the other way about. The belt is turned to run the way its goods travel.
	 */
	private void build(TestServerContext server, BlockPos where, boolean loading) {
		Direction out = towardsTheTrain(where).getOpposite();

		setBlock(server, where, "create:portable_storage_interface[facing="
			+ towardsTheTrain(where).getSerializedName() + "]");

		// Clear the ground the building stands on, both the row the belt runs along and the one above it.
		for (int along = 1; along <= BELT_LENGTH + 2; along++) {
			setBlock(server, where.relative(out, along), "minecraft:air");
			setBlock(server, where.below()
				.relative(out, along), "minecraft:air");
		}

		setBlock(server, stationFunnel(where), "create:andesite_funnel[facing=" + out.getSerializedName()
			+ ",extracting=" + !loading + "]");

		belt(server, beltStart(where), beltEnd(where));

		// Something has to turn it. A belt hangs off shafts at its ends, so the motor stands beside one of
		// them and drives it from there.
		Direction drive = driveSideOf(where);

		setBlock(server, beltEnd(where).relative(drive.getOpposite()),
			"create:creative_motor[facing=" + drive.getSerializedName() + "]");

		setBlock(server, farFunnel(where), "create:andesite_funnel[facing="
			+ out.getOpposite()
				.getSerializedName() + ",extracting=" + loading + "]");
		setBlock(server, chestOf(where), "minecraft:chest");
	}

	private static final int BELT_LENGTH = 5;

	/**
	 * Which way the motor drives a belt from.
	 * <p>
	 * Across the belt rather than along it. A belt hangs off shafts at its ends and those lie across the
	 * way it runs, so a motor put in line with the belt drives nothing - and each of these buildings runs
	 * its belt along a different axis, since each stands on a different side of the square.
	 */
	private Direction driveSideOf(BlockPos where) {
		return towardsTheTrain(where).getClockWise();
	}

	/**
	 * The funnel beside the interface, which is the one the train's goods pass through.
	 * <p>
	 * It stands on the belt, which is the arrangement a funnel and a belt are meant to be in: a funnel over
	 * a belt takes what the belt brings it, or lays what it is given down on it, where one merely standing
	 * beside a belt does neither.
	 */
	private BlockPos stationFunnel(BlockPos where) {
		return where.relative(towardsTheTrain(where).getOpposite());
	}

	/** The belt runs a step below the interface, so that the funnels at its ends are level with it. */
	private BlockPos beltStart(BlockPos where) {
		return stationFunnel(where).below();
	}

	private BlockPos beltEnd(BlockPos where) {
		return beltStart(where).relative(towardsTheTrain(where).getOpposite(), BELT_LENGTH - 1);
	}

	/** The funnel at the far end, standing on the other end of the belt with the chest beside it. */
	private BlockPos farFunnel(BlockPos where) {
		return beltEnd(where).above();
	}

	private BlockPos chestOf(BlockPos where) {
		return farFunnel(where).relative(towardsTheTrain(where).getOpposite());
	}

	/** Turns a building's belt the way its goods have to travel. */
	private void driveTheBelt(TestServerContext server, BlockPos where, boolean towardsTheTrain) {
		BlockPos motor = beltEnd(where).relative(driveSideOf(where)
			.getOpposite());

		server.runOnServer(minecraftServer -> {
			if (minecraftServer.overworld()
				.getBlockEntity(motor) instanceof CreativeMotorBlockEntity be)
				// A belt is laid from the train's end outwards, and that laying is what its plain direction
				// of travel means, so carrying goods back in towards the train is the motor turned the
				// other way about.
				be.generatedSpeed.setValue(towardsTheTrain ? -BELT_RPM : BELT_RPM);
		});
	}

	private static final int BELT_RPM = 32;

	/** Which way the train lies from a building, which is the way its interface has to look. */
	private Direction towardsTheTrain(BlockPos building) {
		return towardsTheCircuit(building).getOpposite();
	}

	private BlockPos loadingSide() {
		return buildingFor(LOADING);
	}

	/**
	 * Which way a train runs past a station.
	 * <p>
	 * The circuit is laid one side at a time and a train follows it round the same way, so a station's
	 * straight is the side of the square it stands on and the way that side was laid is the way the train
	 * goes past it.
	 */
	private Direction travelAt(BlockPos station) {
		if (station.equals(PARKING))
			return SIDES[0];

		return station.equals(LOADING) ? SIDES[1] : SIDES[2];
	}

	/**
	 * Which way a station builds, which is back against the way trains run past it.
	 * <p>
	 * A station puts the front of its train at its own mark and builds the rest away from there, so a train
	 * that comes to rest facing the way it was travelling has been built against it.
	 */
	private Direction assemblyAt(BlockPos station) {
		return travelAt(station).getOpposite();
	}

	/** The side of the track the middle of the circuit is on, which is the side everything is built on. */
	private Direction insideAt(BlockPos station) {
		return towardsTheCircuit(trackFor(station));
	}

	/**
	 * Where a station's building stands.
	 * <p>
	 * Two blocks in from the interface the train presents when it draws up there, which leaves the gap of
	 * one between the pair that they reach across to take hold of one another.
	 */
	private BlockPos buildingFor(BlockPos station) {
		return trainInterfaceAt(station).relative(insideAt(station), 2);
	}

	/**
	 * Where the train's own interface ends up when it has drawn in at a station.
	 * <p>
	 * A train comes to rest at a station exactly where it would have been built there, so this is the
	 * carriage's own plan measured out from the station's stretch of rail. The far straight is the near one
	 * turned right round, so a train standing there is this same arrangement turned about the middle of the
	 * circuit.
	 */
	private BlockPos trainInterfaceAt(BlockPos station) {
		return trackFor(station).relative(assemblyAt(station))
			.above()
			.relative(insideAt(station))
			.relative(assemblyAt(station))
			.above();
	}

	/** Which way the middle of the circuit lies from somewhere on its edge. */
	private Direction towardsTheCircuit(BlockPos pos) {
		int across = MIDDLE_X - pos.getX();
		int along = MIDDLE_Z - pos.getZ();

		if (Math.abs(across) >= Math.abs(along))
			return across > 0 ? Direction.EAST : Direction.WEST;

		return along > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** The middle of the square, which every straight faces. */
	private static final int MIDDLE_X = 70;

	private static final int MIDDLE_Z = 62;

	/**
	 * And the unloading building, across the circuit.
	 * <p>
	 * The far straight is the near one turned right round, so where a train stands there is this one's
	 * position turned about the middle of the circuit.
	 */
	private BlockPos unloadingSide() {
		return buildingFor(UNLOADING);
	}

	/**
	 * Gets the driver out of the train, which is what lets them be moved about again.
	 * <p>
	 * A passenger goes where the thing carrying them goes, so until they are off it nothing that puts the
	 * player somewhere has any effect.
	 */
	private void stepOffTheTrain(ClientGameTestContext context, TestServerContext server) {
		System.out.println("[train] getting off");

		// Letting go of the controls comes first, and it is done the same way they were taken hold of: with
		// another click on them. While they are held, the key that means get off is one of the train's own
		// controls, and the train takes it before the game ever sees it.
		if (driving(context)) {
			clickOnTrain(context, "create:controls", middleOf(whereOnTheTrain(server, "create:controls")));
			context.waitTicks(SETTLE_TICKS);
		}

		assertFalse(driving(context), "Clicking the controls again did not let go of them");

		// And then the key a person presses to get off. Not escape, whatever else it would also do here:
		// that opens the game's own menu, and a paused game does not tick, so the test would sit there
		// waiting on a world that has stopped.
		for (int tries = 0; tries < 6 && riding(context); tries++) {
			context.getInput()
				.holdKeyFor(options -> options.keyShift, 20);
			context.waitTicks(SETTLE_TICKS);
		}

		// Waited for rather than assumed: a passenger goes where the thing carrying them goes, so moving
		// the camera while the player is still aboard does nothing at all.
		assertFalse(riding(context), "The player would not get off the train");
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("stepped_off"));
		System.out.println("[train] off");
	}

	/** Lays a belt between two ends, which is what the belt item does when it is clicked on both. */
	private void belt(TestServerContext server, BlockPos from, BlockPos to) {
		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			BeltConnectorItem.createBelts(level, from, to);
		});
	}

	/** Whether a stationary interface is placed where a train's would take hold of it. */
	private boolean interfacesAreFacing(TestServerContext server, BlockPos stationary, BlockPos onTheTrain) {
		return server.computeOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			BlockState state = level.getBlockState(stationary);

			if (!(state.getBlock() instanceof PortableStorageInterfaceBlock))
				return false;

			Direction facing = state.getValue(PortableStorageInterfaceBlock.FACING);

			// Two along, not one: the pair stand a block apart and reach across the gap between them.
			return stationary.relative(facing, 2)
				.equals(onTheTrain);
		});
	}

	/**
	 * Where the train's own interface has ended up, which is not where it was built unless the train came
	 * back to exactly the same spot.
	 */
	private String whereTheTrainsInterfaceIs(MinecraftServer minecraftServer) {
		for (Train train : Create.RAILWAYS.trains.values())
			for (Carriage carriage : train.carriages) {
				CarriageContraptionEntity entity = carriage.anyAvailableEntity();

				if (entity == null)
					continue;

				for (Map.Entry<BlockPos, StructureBlockInfo> block : entity.getContraption()
					.getBlocks()
					.entrySet())
					if (nameOf(block.getValue()
						.state()).equals("create:portable_storage_interface"))
						return BlockPos.containing(entity.toGlobalVector(
							Vec3.atCenterOf(block.getKey()), 1))
							.toShortString();
			}

		return "nowhere";
	}

	/** How many slots the train's hold has at all, for when nothing appears to be in any of them. */
	private String holdSize(MinecraftServer minecraftServer) {
		List<String> holds = new ArrayList<>();

		for (Train train : Create.RAILWAYS.trains.values())
			for (Carriage carriage : train.carriages)
				holds.add(carriage.storage == null ? "no storage at all"
					: String.valueOf(carriage.storage.getAllItems()
						.size()));

		return holds.toString();
	}

	/** Whether the building's interface has taken hold of the train's, which is what lets goods across. */
	private boolean docked(MinecraftServer minecraftServer) {
		return minecraftServer.overworld()
			.getBlockEntity(loadingSide()) instanceof PortableStorageInterfaceBlockEntity psi
			&& psi.canTransfer();
	}

	private String blockAt(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> nameOf(minecraftServer.overworld()
			.getBlockState(pos)));
	}

	/**
	 * Right up against where the two interfaces meet.
	 * <p>
	 * Close enough to see whether they have taken hold of one another, with the train on one side of them
	 * and the belt that feeds them on the other.
	 */
	private void watchTheCoupling(ClientGameTestContext context, TestServerContext server) {
		Vec3 between = middleOf(loadingSide()).add(middleOf(itemInterface()))
			.scale(0.5);

		ScreenTesting.lookAt(context, server, between, between.add(0, 9, 15));
	}

	/** Stands off a building's corner and above it, so the whole of it is in view. */
	private void watchTheBuilding(ClientGameTestContext context, TestServerContext server, BlockPos where) {
		Direction out = towardsTheTrain(where).getOpposite();

		// Aimed at the interface the train draws up against rather than at the middle of the building, and
		// stood off beyond the far end of it, so the whole run and whatever is on the rails are both in
		// view rather than the building alone.
		ScreenTesting.lookAt(context, server, middleOf(where),
			middleOf(where.relative(out, BELT_LENGTH + 7)).add(0, 7, 10));
	}

	/**
	 * How often the run is looked in on while the train is under way.
	 * <p>
	 * Every tick, because the offer to draw in at a station is a passing thing: a train doing any speed at
	 * all is past a station within a few ticks of being told it is coming up, and taking the offer late
	 * gets an answer about the station after it instead.
	 */
	private static final int WATCH_TICKS = 1;

	/** The drawing-in bar is thirty of the same character; there is nothing to be learnt from repeating it. */
	private static String shorten(String prompt) {
		return prompt.chars()
			.allMatch(character -> character == '|') ? "<drawing in>" : prompt;
	}

	/**
	 * How far off the train has to have got for it to have gone round rather than shuffled about.
	 * <p>
	 * The circuit is a little over twenty across and thirty long, so this is comfortably past the far
	 * straight and comfortably short of the furthest the train can get.
	 */
	private static final double FAR_SIDE = 20;

	/** A lap of the circuit under its own steam takes a while; this is longer than it can need. */
	private static final int DRIVE_TIMEOUT = 1600;

	/**
	 * Puts the player on the ground beside the carriage, on their feet and within reach of it.
	 * <p>
	 * Beside meaning on the outside of the circuit, which is the side with nothing on it, and beside
	 * wherever the train is now rather than where it was built.
	 */
	private void standBeside(ClientGameTestContext context, TestServerContext server) {
		BlockPos controls = whereOnTheTrain(server, "create:controls");
		Direction out = towardsTheCircuit(controls).getOpposite();

		server.runOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);

			player.setGameMode(GameType.CREATIVE);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			player.connection.teleport(controls.getX() + 0.5 + out.getStepX() * 3, RAIL_Y,
				controls.getZ() + 0.5 + out.getStepZ() * 3, 90, 0);
		});
		context.waitTicks(SETTLE_TICKS);
	}

	/** Levels the view off along the track, which is how a driver holds their head. */
	private void lookAlong(ClientGameTestContext context, Direction way) {
		context.getInput()
			.lookAt(way.toYRot(), 0);
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * Looks about until the crosshair is on one of the train's own blocks, then clicks it.
	 * <p>
	 * A rough turn towards where the block stands first, since the sweep only reaches so far to either
	 * side, and then the sweep itself to land on it.
	 */
	private boolean clickOnTrain(ClientGameTestContext context, String wanted, Vec3 roughly) {
		context.runOnClient(client -> {
			Vec3 toBlock = roughly.subtract(client.player.getEyePosition());

			client.player.setYRot((float) (Mth.atan2(toBlock.z, toBlock.x) * 180 / Math.PI) - 90);
			client.player.setXRot(
				(float) -(Mth.atan2(toBlock.y, toBlock.horizontalDistance()) * 180 / Math.PI));
		});

		// Dropped a nudge at a time first, which is how a thing just below the eye is found, and only then
		// swept wider if it was not on the way down.
		if (!ScreenTesting.lookDown(context, client -> aimedAt(client, wanted))
			&& !ScreenTesting.lookAround(context, client -> aimedAt(client, wanted)))
			return false;

		ScreenTesting.rightClick(context);
		context.waitTicks(SETTLE_TICKS);
		return true;
	}

	/** Whether the crosshair is on the named block of the train, asked the way the game itself asks. */
	private static boolean aimedAt(Minecraft client, String wanted) {
		AbstractContraptionEntity train = trainEntity(client);

		if (train == null)
			return false;

		Vec3 origin = client.player.getEyePosition();
		Vec3 target = origin.add(client.player.getLookAngle()
			.scale(client.player.blockInteractionRange()));
		BlockHitResult hit = ContraptionHandlerClient.rayTraceContraption(origin, target, train);

		if (hit == null)
			return false;

		StructureBlockInfo info = train.getContraption()
			.getBlocks()
			.get(hit.getBlockPos());

		return info != null && nameOf(info.state()).equals(wanted);
	}

	private static AbstractContraptionEntity trainEntity(Minecraft client) {
		for (Entity entity : client.level.entitiesForRendering())
			if (entity instanceof CarriageContraptionEntity carriage)
				return carriage;

		return null;
	}

	private static Vec3 trainAt(Minecraft client) {
		AbstractContraptionEntity train = trainEntity(client);

		return train == null ? Vec3.ZERO : train.position();
	}

	private Vec3 trainIsAt(ClientGameTestContext context) {
		return context.computeOnClient(TrainCircuitTest::trainAt);
	}

	private boolean riding(ClientGameTestContext context) {
		return context
			.computeOnClient(client -> client.player.getVehicle() instanceof CarriageContraptionEntity);
	}

	private boolean driving(ClientGameTestContext context) {
		return context.computeOnClient(client -> ControlsHandler.getContraption() != null);
	}

	/** What the train is telling its driver, which is what every step of the run is read from. */
	private static String prompt(Minecraft client) {
		return TrainHUD.currentPrompt == null ? "" : TrainHUD.currentPrompt.getString();
	}

	/** Whether the assembled train carries the named block, which is how the glue is checked. */
	private boolean carriageHolds(TestServerContext server, String wanted) {
		return server.computeOnServer(minecraftServer -> {
			for (Train train : Create.RAILWAYS.trains.values())
				for (Carriage carriage : train.carriages) {
					CarriageContraptionEntity entity = carriage.anyAvailableEntity();

					if (entity == null)
						continue;

					for (StructureBlockInfo info : entity.getContraption()
						.getBlocks()
						.values())
						if (nameOf(info.state()).equals(wanted))
							return true;
				}

			return false;
		});
	}

	/** Everything the assembled train carries, for when something was left behind. */
	private String carriageContents(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			List<String> carried = new ArrayList<>();

			for (Train train : Create.RAILWAYS.trains.values())
				for (Carriage carriage : train.carriages) {
					CarriageContraptionEntity entity = carriage.anyAvailableEntity();

					if (entity == null)
						continue;

					for (StructureBlockInfo info : entity.getContraption()
						.getBlocks()
						.values())
						carried.add(nameOf(info.state()));
				}

			return carried.toString();
		});
	}

	/** What is still standing where the carriage was built, which is whatever the train did not take. */
	private String leftBehind(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			List<String> standing = new ArrayList<>();

			for (int across = -1; across <= 1; across++)
				for (int along = -1; along <= 1; along++)
					for (int up = 0; up <= 1; up++) {
						BlockPos pos = floor(across, along).above(up);
						BlockState state = level.getBlockState(pos);

						if (!state.isAir())
							standing.add(pos.toShortString() + " " + nameOf(state));
					}

			return standing.toString();
		});
	}

	/** What the assembled train ended up called, which is whatever a station reports when it is there. */
	private String theTrainsName(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> Create.RAILWAYS.trains.values()
			.iterator()
			.next().name.getString());
	}

	/** Which train, if any, is standing at a station. */
	private String trainWaitingAt(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(minecraftServer -> {
			BlockEntity be = minecraftServer.overworld()
				.getBlockEntity(pos);

			if (!(be instanceof StationBlockEntity station))
				return "there is no station at " + pos;

			GlobalStation global = station.getStation();

			if (global == null)
				return "the station is not on the track";

			Train train = global.getPresentTrain();

			return train == null ? "no train is there" : train.name.getString();
		});
	}

	private static String nameOf(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock())
			.toString();
	}

	private static Vec3 middleOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	/**
	 * Makes sure the game's own display is up.
	 * <p>
	 * What the train tells its driver is shown there, and so is everything Create says about taking hold of
	 * the controls or letting go of them, so a run watched with it turned off says nothing about itself.
	 */
	private void showTheHud(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});
	}

	/** Stands off the carriage's corner and above it, so the floor and everything on it are in view. */
	private void watchTheTrain(ClientGameTestContext context, TestServerContext server) {
		showTheHud(context);

		ScreenTesting.lookAt(context, server, middleOf(bogey(0).above()),
			new Vec3(bogey(0).getX() + 6.5, bogey(0).getY() + 5, bogey(0).getZ() + 6.5));
	}

	/** Turns the station over to assembly through its own screen, then gets out of the way. */
	private void enterAssemblyMode(ClientGameTestContext context, TestServerContext server) {
		ScreenTesting.rightClickBlock(context, server, PARKING);
		ScreenTesting.waitForScreen(context, StationScreen.class);

		ScreenTesting.click(context, ScreenTesting.widget(context, "newTrainButton"));
		ScreenTesting.waitForScreen(context, AssemblyScreen.class);

		ScreenTesting.closeWithEscape(context);

		// The station only works out how far it can build, and marks out the stretch of rail a click may
		// put a bogey on, on one of its slower ticks. Clicking before that lands on nothing.
		context.waitTicks(SETTLE_TICKS * 4);
	}

	/** Opens the station again - which now offers the assembly screen - and presses the button. */
	private void assemble(ClientGameTestContext context, TestServerContext server) {
		ScreenTesting.rightClickBlock(context, server, PARKING);
		ScreenTesting.waitForScreen(context, AssemblyScreen.class);
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.click(context, ScreenTesting.widget(context, "toggleAssemblyButton"));
		context.waitTicks(SETTLE_TICKS * 3);

		if (ScreenTesting.screenIsOpen(context))
			ScreenTesting.closeWithEscape(context);

		context.waitTicks(SETTLE_TICKS);
	}

	/** Puts a block against the side of another by clicking that face square on. */
	private void placeAgainst(ClientGameTestContext context, TestServerContext server, BlockPos target,
		String item, Direction side) {
		server.runCommand("item replace entity @a hotbar.0 with " + item);
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		Vec3 middle = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
		Vec3 from = new Vec3(target.getX() + 0.5 + side.getStepX() * 3, target.getY() + 0.5,
			target.getZ() + 0.5 + side.getStepZ() * 3);

		ScreenTesting.rightClickAt(context, server, middle, from, target);
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * The carriage: a floor three squares by three with the bogey at its middle, and the fittings on it.
	 * <p>
	 * Where each piece goes matters. The controls have to face the way the train is built or the station
	 * refuses the whole thing, and the seat has to be the square they face. Those two take the left-hand
	 * side with nothing standing over them, the cargo goes across the back, and the two interfaces take the
	 * right - which is the side facing into the circuit, where whatever loads and empties the train will
	 * stand.
	 */
	private void buildCarriage(TestServerContext server) {
		for (int across = -1; across <= 1; across++)
			for (int along = -1; along <= 1; along++)
				if (across != 0 || along != 0)
					setBlock(server, floor(across, along), CASING);

		setBlock(server, controls(), "create:controls[facing=" + ASSEMBLY.getSerializedName() + "]");
		setBlock(server, seat(), "create:red_seat");

		setBlock(server, itemInterface(), "create:portable_storage_interface[facing=" + INTERFACES + "]");
		setBlock(server, fluidInterface(), "create:portable_fluid_interface[facing=" + INTERFACES + "]");

		setBlock(server, chest(), "minecraft:chest");
		setBlock(server, tank(), "create:fluid_tank");
	}

	private static final String CASING = "create:andesite_casing";

	/** The train's right-hand side, which is the one that faces in towards the middle of the circuit. */
	private static final String INTERFACES = "east";

	/**
	 * A square of the carriage floor, counted out from the bogey at its middle.
	 * <p>
	 * Across is to the right of the way the train is built, and along is the way it is built, so along -1
	 * is towards the front of the train.
	 */
	private BlockPos floor(int across, int along) {
		return bogey(0).relative(Direction.EAST, across)
			.relative(ASSEMBLY, -along);
	}

	/**
	 * The controls, in the middle of the left-hand side, facing the way the train is built.
	 * <p>
	 * Not a free choice either way. A station refuses to assemble anything unless some set of controls
	 * faces the way it is building, and it only counts a seat as the driver's when the controls beside it
	 * face that seat - so the controls face along the train and the seat is the square ahead of them.
	 */
	private BlockPos controls() {
		return floor(-1, 0).above();
	}

	/** The seat, which is the square the controls face, and where the driver goes. */
	private BlockPos seat() {
		return floor(-1, -1).above();
	}

	private BlockPos tank() {
		return floor(-1, 1).above();
	}

	/**
	 * The hold, directly behind the interface that fills it.
	 * <p>
	 * Behind meaning on the train's side of it: the interface reaches out one way to whatever the station
	 * has waiting, and puts what it takes into the square at its back.
	 */
	private BlockPos chest() {
		return floor(0, -1).above();
	}

	private BlockPos itemInterface() {
		return floor(1, -1).above();
	}

	private BlockPos fluidInterface() {
		return floor(1, 0).above();
	}

	private void clickTrack(ClientGameTestContext context, TestServerContext server, BlockPos track) {
		ScreenTesting.rightClickAt(context, server, surfaceOf(track), standingBack(track, ASSEMBLY), track);
		context.waitTicks(SETTLE_TICKS);
	}

	/** Puts a block on top of another by clicking its upper face, facing the given way while doing it. */
	private void placeBlockOn(ClientGameTestContext context, TestServerContext server, BlockPos below,
		String item, Direction facing) {
		server.runCommand("item replace entity @a hotbar.0 with " + item);
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		ScreenTesting.rightClickAt(context, server, topOf(below), standingBack(below, facing), below);
		context.waitTicks(SETTLE_TICKS);
	}

	/**
	 * Which way a train grows out of station A.
	 * <p>
	 * Not a free choice: the station marks out the stretch of rail it will build over, and that runs back
	 * against the way the rail was pointed at when the station was put down.
	 */
	private static final Direction ASSEMBLY = Direction.NORTH;

	/** The rail a bogey is put on, counted out from the piece the station watches. */
	private BlockPos bogeyTrack(int offset) {
		return trackFor(PARKING).relative(ASSEMBLY, offset + 1);
	}

	/** Where that bogey ends up: one above its rail. */
	private BlockPos bogey(int offset) {
		return bogeyTrack(offset).above();
	}

	private int trainCount(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> Create.RAILWAYS.trains.size());
	}

	private String bogeyOffsets(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var be = (StationBlockEntity) minecraftServer.overworld()
				.getBlockEntity(PARKING);
			int[] found = (int[]) ScreenTesting.read(be, "bogeyLocations");

			return java.util.Arrays.toString(found);
		});
	}

	private String assemblyLength(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> String.valueOf(ScreenTesting.read(
			minecraftServer.overworld()
				.getBlockEntity(PARKING),
			"assemblyLength")));
	}

	private String assemblyState(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var level = minecraftServer.overworld();
			var be = level.getBlockEntity(PARKING);
			var area = com.simibubi.create.content.trains.station.StationBlockEntity.assemblyAreas.get(level)
				.get(PARKING);

			return "assembling=" + level.getBlockState(PARKING)
				+ " direction=" + ScreenTesting.read(be, "assemblyDirection") + " area=" + area
				+ " target=" + trackFor(PARKING) + " bogeyRail=" + bogeyTrack(0)
				+ " bogey=" + level.getBlockState(bogey(0));
		});
	}

	private String lastComplaint(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var be = (StationBlockEntity) minecraftServer.overworld()
				.getBlockEntity(PARKING);

			try {
				var complaint = (com.simibubi.create.content.contraptions.AssemblyException) ScreenTesting
					.read(be, "lastException");

				return complaint.component.getString();
			} catch (Throwable nothingWrong) {
				return "nothing";
			}
		});
	}

	/**
	 * Puts a station beside the track, the way one is put down in play.
	 * <p>
	 * Two clicks: the rail itself, which is what the station will watch and which way along it a train is
	 * built, and then the ground a couple of blocks to the side, which is where the station lands.
	 */
	/** Puts a station down and gives it its name while the player is still standing at it. */
	private void placeAndName(ClientGameTestContext context, TestServerContext server, BlockPos pos,
		String name) {
		placeStation(context, server, pos, trackFor(pos), travelAt(pos));
		nameStation(context, server, pos, name);
	}

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
		if (station.equals(PARKING))
			return new BlockPos(52, RAIL_Y, 62);

		return station.equals(LOADING) ? new BlockPos(70, RAIL_Y, 80) : new BlockPos(88, RAIL_Y, 62);
	}

	/** Beside each straight, a couple of blocks clear of the rails. */
	/**
	 * Where the train is built and where it comes back to. Nothing is loaded or unloaded here.
	 * <p>
	 * It has the middle of the straight because it is the one that has to assemble a train, and a station
	 * can only build over the run of plain track ahead of it - which ends at the next station's mark, or at
	 * the first bend.
	 */
	private static final BlockPos PARKING = new BlockPos(50, RAIL_Y, 62);


	/** The next straight round: where goods are put aboard. */
	private static final BlockPos LOADING = new BlockPos(70, RAIL_Y, 82);

	/** And the one after that, where they are taken off again. */
	private static final BlockPos UNLOADING = new BlockPos(90, RAIL_Y, 62);

	private static final String PARKING_NAME = "Parking Station";

	private static final String LOADING_NAME = "Loading Station";

	private static final String UNLOADING_NAME = "Unloading Station";

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
		showTheHud(context);

		ScreenTesting.lookAt(context, server, new Vec3(MIDDLE_X, RAIL_Y, MIDDLE_Z),
			new Vec3(MIDDLE_X, RAIL_Y + 44, MIDDLE_Z + 52));
	}

	/** Just inside the block, low enough to be on the rails rather than in the air above them. */
	private static Vec3 surfaceOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
	}

	private static Vec3 topOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
