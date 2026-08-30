package com.simibubi.create.gametest.client.transportation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * A crate of andesite alloy sent from one chest to another by Create's package logistics.
 * <p>
 * The route is the one the mod is built around: a packager empties the first chest into an addressed
 * cardboard package, the frogport above it throws the package onto a chain conveyor, the chain
 * carries it to the far post, the frogport there catches it because the address matches its filter,
 * and a chute drops it into a second packager that unwraps it into the second chest.
 * <p>
 * Everything a player would do with a wrench and a coil of chain is done here by hand, which is
 * worth knowing about if this ever breaks: the two posts are strung together by putting each one's
 * position into the other's connection set, and each frogport is told which post it hangs from.
 */
public class LogisticsTest {

	/** The chest at each end. Everything else is stacked above it. */
	private static final BlockPos SEND = new BlockPos(0, -60, 0);
	private static final BlockPos RECEIVE = new BlockPos(8, -61, 0);

	private static final String ADDRESS = "warehouse";
	private static final int COUNT = 32;

	/** Roughly a minute, which is far longer than the journey takes. */
	private static final int PATIENCE_TICKS = 600;

	@ClientGameTest(screenshot = false)
	@DisplayName("A chain conveyor carries a package from one chest to another")
	void chestToChest(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		// Sending: chest, packager, frogport, post. The packager looks up, so it takes from the chest
		// below it and hands what it packs to the frogport above.
		setBlock(server, SEND, "minecraft:chest");
		setBlock(server, SEND.above(1), "create:packager[facing=up]");
		setBlock(server, SEND.above(2), "create:package_frogport");
		setBlock(server, SEND.above(3), "create:chain_conveyor");
		setBlock(server, SEND.above(4), "create:creative_motor[facing=down]");

		// Receiving: the same in reverse, with a hopper between the port and the packager. Nothing in a
		// frogport pushes what it catches anywhere, and a packager only unwraps what is put into it, so
		// something has to carry the package the one block between them. A chute looks like the Create
		// answer but does not pull from a frogport; a hopper does.
		setBlock(server, RECEIVE, "minecraft:chest");
		setBlock(server, RECEIVE.above(1), "create:packager[facing=up]");
		setBlock(server, RECEIVE.above(2), "minecraft:hopper[facing=down]");
		setBlock(server, RECEIVE.above(3), "create:package_frogport");
		setBlock(server, RECEIVE.above(4), "create:chain_conveyor");

		server.runCommand("item replace block %d %d %d container.0 with create:andesite_alloy %d"
			.formatted(SEND.getX(), SEND.getY(), SEND.getZ(), COUNT));

		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			BlockPos postA = SEND.above(3);
			BlockPos postB = RECEIVE.above(4);

			ChainConveyorBlockEntity a = chain(level, postA);
			ChainConveyorBlockEntity b = chain(level, postB);
			a.connections.add(postB.subtract(postA));
			b.connections.add(postA.subtract(postB));

			// The length and angle of each connection is worked out once and cached, and that happened
			// before these connections existed, so it has to be thrown away and done again.
			a.connectionStats = null;
			b.connectionStats = null;
			a.prepareStats();
			b.prepareStats();
			a.notifyUpdate();
			b.notifyUpdate();

			// Each frogport hangs on the ring around the post directly above it.
			port(level, SEND.above(2)).target =
				new ChainConveyorFrogportTarget(new BlockPos(0, 1, 0), 0, Optional.empty(), false);

			PackagePortBlockEntity receiver = port(level, RECEIVE.above(3));
			receiver.target =
				new ChainConveyorFrogportTarget(new BlockPos(0, 1, 0), 0, Optional.empty(), false);
			receiver.addressFilter = ADDRESS;
			receiver.filterChanged();
		});

		watchFromTheSide(context, server);
		// Long enough for the posts to tell each other which addresses they can reach.
		context.waitTicks(60);
		context.takeScreenshot(shot("logistics_before"));

		server.runOnServer(minecraftServer -> {
			PackagerBlockEntity packager = (PackagerBlockEntity) minecraftServer.overworld()
				.getBlockEntity(SEND.above(1));

			// What an order from a stock keeper eventually becomes. The packager takes requests off the
			// list as it fills them, so it has to be a list it can modify.
			packager.attemptToSend(new ArrayList<>(List.of(PackagingRequest.create(
				AllItems.ANDESITE_ALLOY.asStack(COUNT), COUNT, ADDRESS, 0, new MutableBoolean(true), 0, 0,
				null))));
		});

		context.waitTicks(100);
		context.takeScreenshot(shot("logistics_in_transit"));

		int waited = 100;
		while (waited < PATIENCE_TICKS && delivered(server) == 0) {
			context.waitTicks(20);
			waited += 20;
		}

		context.takeScreenshot(shot("logistics_after"));

		context.runOnClient(client -> {
			if (client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		assertEquals(COUNT, delivered(server),
			"The andesite alloy never arrived in the far chest after " + waited + " ticks");
		assertEquals(0, remaining(server), "The near chest should have been emptied into the package");
	}

	/** How much andesite alloy is sitting in the far chest. */
	private static int delivered(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> countIn(minecraftServer.overworld(), RECEIVE));
	}

	private static int remaining(TestServerContext server) {
		return server.computeOnServer(minecraftServer -> countIn(minecraftServer.overworld(), SEND));
	}

	/**
	 * Counted straight off the chest rather than through an item handler, because this runs on the
	 * server thread while it is busy and opening a second transaction there is not allowed.
	 */
	private static int countIn(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof Container container))
			return 0;

		int found = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (AllItems.ANDESITE_ALLOY.isIn(stack))
				found += stack.getCount();
		}
		return found;
	}

	/**
	 * Stands the camera off to the south so both posts, the chain slung between them and the chests
	 * underneath are all in frame.
	 */
	private static void watchFromTheSide(ClientGameTestContext context, TestServerContext server) {
		context.runOnClient(client -> {
			if (!client.gui.hud.isHidden())
				client.gui.hud.toggle();
		});

		double centreX = (SEND.getX() + RECEIVE.getX()) / 2.0 + 0.5;
		double centreY = RECEIVE.getY() + 2.5;
		double centreZ = SEND.getZ() + 0.5;

		double eyeX = centreX;
		double eyeY = centreY + 4;
		double eyeZ = centreZ + 13;

		double toY = centreY - eyeY;
		double toZ = centreZ - eyeZ;
		double pitch = Math.toDegrees(-Math.atan2(toY, Math.abs(toZ)));

		// Spectator so the camera can hang in the air with nothing of the player in the way.
		server.runCommand("gamemode spectator @a");

		double eyeHeight = server.computeOnServer(minecraftServer -> (double) minecraftServer
			.getPlayerList()
			.getPlayers()
			.getFirst()
			.getEyeHeight());

		// Looking north, since the camera stands to the south of everything.
		server.runCommand("tp @a %.2f %.2f %.2f 180 %.2f".formatted(eyeX, eyeY - eyeHeight, eyeZ, pitch));
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

	private static void setBlock(TestServerContext server, BlockPos pos, String state) {
		server.runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
	}

	private static ChainConveyorBlockEntity chain(ServerLevel level, BlockPos pos) {
		return (ChainConveyorBlockEntity) level.getBlockEntity(pos);
	}

	private static PackagePortBlockEntity port(ServerLevel level, BlockPos pos) {
		return (PackagePortBlockEntity) level.getBlockEntity(pos);
	}

}
