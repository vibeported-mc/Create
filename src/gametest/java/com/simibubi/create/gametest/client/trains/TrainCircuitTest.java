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
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
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

	@ClientGameTest(screenshot = false)
	@Order(3)
	@DisplayName("A train is built on the rails at station A and assembled from its screen")
	void buildsATrainAtStationA(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals("Station A", stationName(server, STATION_A),
			"There is no named station to build a train at - did the station phase fail?");

		enterAssemblyMode(context, server);

		// A bogey appears on the rail wherever the marked stretch is clicked. One is enough for a carriage,
		// and every bogey put down needs a body of its own hung off it.
		server.runCommand("item replace entity @a hotbar.0 with create:railway_casing");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));
		context.waitTicks(SETTLE_TICKS);

		clickTrack(context, server, bogeyTrack(0));

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
	@DisplayName("The train is ridden a full circuit by hand and parked back at station A")
	void ridesTheTrainRoundTheCircuit(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		assertEquals(1, trainCount(server), "There is no train to ride - did the building phase fail?");

		standBeside(context, server);

		// Sitting down and taking the controls are both clicks on the train itself, which is an entity
		// rather than blocks in the world, so what the crosshair is on has to be looked for rather than
		// worked out.
		assertTrue(clickOnTrain(context, "create:red_seat", middleOf(seat())),
			"Could not find the seat to sit on");
		assertTrue(riding(context), "Clicking the seat did not sit the player on the train");

		// The seat is the square the controls face, so from it they are the one behind: the view is turned
		// back down the train, levelled off, and then dropped a nudge at a time until it finds them. If
		// they are not on the way down - a seated head does not sit quite where a standing one does - the
		// wider sweep picks them up.
		lookAlong(context, ASSEMBLY.getOpposite());

		boolean onTheControls = ScreenTesting.lookDown(context, client -> aimedAt(client, "create:controls"))
			|| ScreenTesting.lookAround(context, client -> aimedAt(client, "create:controls"));

		assertTrue(onTheControls,
			"Could not find the controls from the seat; the crosshair was on " + whatIsUnderTheCrosshair(context));

		ScreenTesting.rightClick(context);
		context.waitTicks(SETTLE_TICKS);
		assertTrue(driving(context), "Clicking the controls did not hand the player the train");

		Vec3 setOffFrom = trainIsAt(context);

		// From here nothing is asked of the server: the key is held down for real and the client works out
		// what to send from it, the same as under anyone else's hands.
		context.getInput()
			.holdKey(options -> options.keyUp);

		// Which way that turns out to be is the train's business rather than this test's - the controls
		// face the way the station built, which need not be the way the train pulls away - so the camera is
		// turned to wherever it has actually started going, and the driver watches the track ahead.
		context.waitFor(client -> trainAt(client).distanceTo(setOffFrom) > 2, DRIVE_TIMEOUT);
		lookAlong(context, wayItIsGoing(context, setOffFrom));
		context.takeScreenshot(shot("train_at_the_controls"));

		// Right round. Away down the far side first, so that coming back within reach of station A means a
		// lap of the circuit rather than never having left it; then the train says for itself when the
		// station is close enough to hand the last of the run over to, and holding the same key a driver
		// would hands it over. Everything read here is read off the client.
		List<String> heard = new ArrayList<>();
		Vec3 wasAt = setOffFrom;
		double furthest = 0;
		boolean beenAway = false;
		boolean seenItTurning = false;
		boolean askedToStop = false;
		boolean arrived = false;

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
				context.takeScreenshot(shot("train_on_a_corner"));
				watchOverTheShoulder(context, false);
				seenItTurning = true;
			}

			if (!saying.isEmpty() && !heard.contains(shorten(saying)))
				heard.add(shorten(saying));

			if (!beenAway && away > FAR_SIDE) {
				beenAway = true;
				context.takeScreenshot(shot("train_far_side"));
			}

			// Nothing here measures how close the station is: the train says when it is near enough to hand
			// the rest of the run to, and having been away already means saying so means a lap.
			if (beenAway && !askedToStop && saying.contains("approach Station A")) {
				context.takeScreenshot(shot("train_approaching"));
				context.getInput()
					.holdKey(options -> options.keyJump);
				askedToStop = true;
			}

			arrived = askedToStop && saying.contains("Arrived at Station A");
		}

		String run = "; it got " + Math.round(furthest) + " blocks off and heard " + heard;

		context.getInput()
			.releaseKey(options -> options.keyUp);
		context.getInput()
			.releaseKey(options -> options.keyJump);
		context.waitTicks(SETTLE_TICKS);
		context.takeScreenshot(shot("train_parked"));

		assertTrue(beenAway, "The train never got away from station A" + run);
		assertTrue(askedToStop, "The train went round but never offered to draw in at station A" + run);
		assertTrue(arrived, "The train was asked to draw in at station A but never got there" + run);

		assertEquals(theTrainsName(server), trainWaitingAt(server, STATION_A),
			"The train did not end up parked at the station it set off from" + run);
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

	/** How often the run is looked in on while the train is under way. */
	private static final int WATCH_TICKS = 5;

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
	private static final int DRIVE_TIMEOUT = 4000;

	/** Puts the player on the ground beside the carriage, on their feet and within reach of it. */
	private void standBeside(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(minecraftServer -> {
			ServerPlayer player = minecraftServer.getPlayerList()
				.getPlayers()
				.get(0);

			player.setGameMode(GameType.CREATIVE);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
			player.connection.teleport(controls().getX() - 2.5, RAIL_Y, controls().getZ() + 0.5, 90, 0);
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

		if (!ScreenTesting.lookAround(context, client -> aimedAt(client, wanted)))
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

	private void hideTheHud(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});
	}

	/** Stands off the carriage's corner and above it, so the floor and everything on it are in view. */
	private void watchTheTrain(ClientGameTestContext context, TestServerContext server) {
		hideTheHud(context);

		ScreenTesting.lookAt(context, server, middleOf(bogey(0).above()),
			new Vec3(bogey(0).getX() + 6.5, bogey(0).getY() + 5, bogey(0).getZ() + 6.5));
	}

	/** Turns the station over to assembly through its own screen, then gets out of the way. */
	private void enterAssemblyMode(ClientGameTestContext context, TestServerContext server) {
		ScreenTesting.rightClickBlock(context, server, STATION_A);
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
		ScreenTesting.rightClickBlock(context, server, STATION_A);
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

	private BlockPos chest() {
		return floor(0, 1).above();
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
		return trackFor(STATION_A).relative(ASSEMBLY, offset + 1);
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
				.getBlockEntity(STATION_A);
			int[] found = (int[]) ScreenTesting.read(be, "bogeyLocations");

			return java.util.Arrays.toString(found);
		});
	}

	private String assemblyLength(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> String.valueOf(ScreenTesting.read(
			minecraftServer.overworld()
				.getBlockEntity(STATION_A),
			"assemblyLength")));
	}

	private String assemblyState(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var level = minecraftServer.overworld();
			var be = level.getBlockEntity(STATION_A);
			var area = com.simibubi.create.content.trains.station.StationBlockEntity.assemblyAreas.get(level)
				.get(STATION_A);

			return "assembling=" + level.getBlockState(STATION_A)
				+ " direction=" + ScreenTesting.read(be, "assemblyDirection") + " area=" + area
				+ " target=" + trackFor(STATION_A) + " bogeyRail=" + bogeyTrack(0)
				+ " bogey=" + level.getBlockState(bogey(0));
		});
	}

	private String lastComplaint(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> {
			var be = (StationBlockEntity) minecraftServer.overworld()
				.getBlockEntity(STATION_A);

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

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
