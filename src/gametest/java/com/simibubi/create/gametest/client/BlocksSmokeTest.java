package com.simibubi.create.gametest.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.provider.MethodSource;

import com.simibubi.create.Create;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTests;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places every block Create gives the player, on its own in mid air, and photographs it from every
 * side.
 * <p>
 * Nothing here asserts what the picture should look like. The point is that a block whose model,
 * block entity renderer or Flywheel instance is broken tends not to fail quietly - it throws while
 * the chunk is built or the frame is drawn, and the run stops. What the pictures are for is the
 * quieter half: a missing model, an untextured face, a block entity that renders in the wrong place.
 * Those need eyes, and this is what gives them something to look at.
 * <p>
 * This is the long one. It runs one client and one world for the whole class, so the cost is roughly
 * a second per block rather than the three a fresh world would add. Narrow it while working on
 * something in particular:
 * <pre>
 * gradlew runClientGameTest -PclientGameTestSelect=com.simibubi.create.gametest.client.BlocksSmokeTest
 * </pre>
 * Every block is four pictures at 1280x960, so a full run leaves the better part of a gigabyte under
 * build/run/clientGameTest/screenshots. {@link #WIDTH} and {@link #HEIGHT} are where to trade that
 * away.
 */
@SharedWorld
@Tag("blocks")
public class BlocksSmokeTest {

	/**
	 * High enough that the ground falls outside the fog and every shot has plain sky behind it, which
	 * is the whole reason for the altitude - a block against a green hillside is much harder to read.
	 */
	private static final BlockPos ORIGIN = new BlockPos(0, 200, 0);

	/**
	 * Narrower than the game's usual 70, because a wide angle at close range bends a cube out of shape
	 * and this is meant to show what the block looks like.
	 */
	private static final int FOV = 40;

	/**
	 * How much of the frame's height the subject should take up. The camera distance follows from this
	 * and from how big the subject is, so one block and a two by two wall are both framed the same.
	 */
	private static final double FILL = 0.85;

	/**
	 * The four corners that between them see all six faces: two above, and the same two below.
	 */
	private static final List<View> VIEWS = List.of(
		new View("se_above", 1, 1, 1),
		new View("nw_above", -1, 1, -1),
		new View("se_below", 1, -1, 1),
		new View("nw_below", -1, -1, -1));

	/**
	 * Far larger than the window the test runs in, which the screenshot code allows because it resizes
	 * the render target rather than the window. These are meant to be looked at closely - a face with
	 * the wrong texture on it is not something you can see in a thumbnail - and the cost is disk.
	 */
	private static final int WIDTH = 1280;
	private static final int HEIGHT = 960;

	/** Two ticks, near enough a tenth of a second, for the frame to settle before it is captured. */
	private static final int SETTLE = 2;

	/** Half the height of the frame at one block away, from which the camera distance is worked out. */
	private static final double HALF_FRAME = Math.tan(Math.toRadians(FOV * FILL / 2));

	/**
	 * The sixteen dye colours, in the order a block family is registered in.
	 */
	private static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow",
		"lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

	/**
	 * Every block Create gives the player, minus the repetitions.
	 * <p>
	 * Blocks with no item are the ones the mod places itself - the halves of a large water wheel, the
	 * body of a multiblock - and they mean nothing standing alone. The dyed families are worse than
	 * useless: a hundred and twelve pictures of the same six models in different colours, which nobody
	 * is going to look through and which would hide the blocks that matter. One of each family stands
	 * for the rest, and where there is an undyed block already - a nixie tube - that one does.
	 */
	static List<String> blocks() {
		List<String> ids = new ArrayList<>();

		for (Block block : BuiltInRegistries.BLOCK) {
			if (block.asItem() == Items.AIR) {
				continue;
			}

			Identifier id = BuiltInRegistries.BLOCK.getKey(block);

			if (id != null && Create.ID.equals(id.getNamespace())) {
				ids.add(id.toString());
			}
		}

		ids.sort(null);
		return ids.stream().filter(id -> standsForItsColours(id, ids)).toList();
	}

	private static boolean standsForItsColours(String id, List<String> all) {
		String path = id.substring(id.indexOf(':') + 1);

		for (String dye : DYES) {
			if (!path.startsWith(dye + "_")) {
				continue;
			}

			String undyed = Create.ID + ":" + path.substring(dye.length() + 1);
			return !all.contains(undyed) && DYES.getFirst().equals(dye);
		}

		return true;
	}

	/**
	 * How far the camera sits above the position a teleport puts the player at. Asked of the game
	 * rather than assumed, because getting it wrong tilts every shot.
	 */
	private double eyeHeight;

	@BeforeEach
	void watchFromNowhere(ClientGameTestContext context, TestServerContext server) {
		// Spectator so the camera can sit in mid air without falling, and so no hand gets into the shot.
		server.runCommand("gamemode spectator @a");

		// The hotbar, the crosshair and whatever a mod like JourneyMap draws over the corner are all
		// in the way of the thing being photographed.
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}

			client.options.fov().set(FOV);
		});

		eyeHeight = server.computeOnServer(minecraftServer -> (double) minecraftServer.getPlayerList()
			.getPlayers()
			.getFirst()
			.getEyeHeight());
	}

	@AfterEach
	void putTheClientBack(ClientGameTestContext context) {
		// These are the client's own settings, not this class's, and the next test class to run should
		// not inherit a hidden HUD and a telephoto lens from it.
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}

			client.options.fov().set(70);
		});
	}

	@ClientGameTests(screenshot = false)
	@MethodSource("blocks")
	void block(String id, ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill %d %d %d %d %d %d air".formatted(ORIGIN.getX() - 2, ORIGIN.getY() - 2,
			ORIGIN.getZ() - 2, ORIGIN.getX() + 3, ORIGIN.getY() + 3, ORIGIN.getZ() + 2));

		Subject subject = Subject.of(id);
		subject.place(server, id);

		// Long enough for the section to be rebuilt and the block entity, if there is one, to be set up.
		context.waitTicks(5 + SETTLE);

		Placed placed = inspect(server, id);

		if (!placed.present()) {
			// Rails, seats, anything that wants something underneath it. Nothing to photograph, and
			// nothing broken either.
			Assumptions.abort(id + " cannot stand in mid air");
		}

		String name = id.replace(':', '_');

		for (View view : subject.views(placed.solid())) {
			view.lookAt(server, subject, eyeHeight);
			context.waitTicks(SETTLE);
			context.takeScreenshot(TestScreenshotOptions.of(name + "_" + view.name())
				.withSize(WIDTH, HEIGHT));
		}

		// Leave the client a moment to itself before the next block arrives.
		context.waitTicks(SETTLE);
	}

	/**
	 * What is actually standing at the origin, and whether it is a plain cube.
	 *
	 * @param present whether the block survived being put in mid air at all
	 * @param solid whether it fills its space and hides what is behind it, which is Create's way of
	 *              saying the model is an ordinary six sided box
	 */
	private record Placed(boolean present, boolean solid) {
	}

	private static Placed inspect(TestServerContext server, String id) {
		return server.computeOnServer(minecraftServer -> {
			Block expected = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
			BlockState state = minecraftServer.overworld().getBlockState(ORIGIN);
			return new Placed(state.is(expected), state.canOcclude());
		});
	}

	/**
	 * What gets built, which is not always one block: a window is glass that joins up with its
	 * neighbours, so on its own it says nothing about how it looks in a wall.
	 */
	private enum Subject {
		/** One block, seen from all four corners. */
		SINGLE,
		/** Two by two, so the panes join, and one corner is enough to show it. */
		PANES,
		/** Two by two, seen from all four corners. */
		WINDOW;

		static Subject of(String id) {
			if (id.endsWith("_window_pane")) {
				return PANES;
			}

			return id.endsWith("_window") ? WINDOW : SINGLE;
		}

		void place(TestServerContext server, String id) {
			setBlock(server, ORIGIN, id);

			if (this == SINGLE) {
				return;
			}

			setBlock(server, ORIGIN.east(), id);
			setBlock(server, ORIGIN.above(), id);
			setBlock(server, ORIGIN.east().above(), id);
		}

		/**
		 * One corner is enough for a plain cube, whose remaining faces hold no surprises, and for a
		 * wall of panes, which is only interesting from the front. Everything else earns all four.
		 */
		List<View> views(boolean solid) {
			return this == PANES || (this == SINGLE && solid) ? VIEWS.subList(0, 1) : VIEWS;
		}

		/** The middle of what was built, which the camera points at. */
		double centreX() {
			return this == SINGLE ? ORIGIN.getX() + 0.5 : ORIGIN.getX() + 1.0;
		}

		double centreY() {
			return this == SINGLE ? ORIGIN.getY() + 0.5 : ORIGIN.getY() + 1.0;
		}

		double centreZ() {
			return ORIGIN.getZ() + 0.5;
		}

		/** Half the corner to corner size of what was built, seen from a corner. */
		double radius() {
			return this == SINGLE ? Math.sqrt(3) / 2 : Math.sqrt(2 * 2 + 2 * 2 + 1) / 2;
		}

		/** Far enough back that the subject fills {@link #FILL} of the frame, and no further. */
		double distance() {
			return radius() / HALF_FRAME;
		}
	}

	private record View(String name, int x, int y, int z) {

		void lookAt(TestServerContext server, Subject subject, double eyeHeight) {
			// The corner is a unit diagonal, so each axis gets its share of the distance rather than
			// all of it, which would put the camera nearly twice as far away as intended.
			double step = subject.distance() / Math.sqrt(3);

			// Where the camera itself has to end up, which is not where the player is put: a teleport
			// places the feet, and the view is a head's height above them.
			double eyeX = subject.centreX() + x * step;
			double eyeY = subject.centreY() + y * step;
			double eyeZ = subject.centreZ() + z * step;

			double toX = subject.centreX() - eyeX;
			double toY = subject.centreY() - eyeY;
			double toZ = subject.centreZ() - eyeZ;
			double flat = Math.sqrt(toX * toX + toZ * toZ);

			// teleport ... facing would do this, but it aims from the wrong height and leaves every
			// shot tilted, so the angles are worked out here against the eye position above.
			server.runCommand("tp @a %.3f %.3f %.3f %.3f %.3f".formatted(eyeX, eyeY - eyeHeight, eyeZ,
				Math.toDegrees(Math.atan2(-toX, toZ)), Math.toDegrees(-Math.atan2(toY, flat))));
		}
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String id) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), id));
	}

}
