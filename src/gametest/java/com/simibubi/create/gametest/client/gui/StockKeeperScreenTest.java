package com.simibubi.create.gametest.client.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.junit.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.junit.SharedWorld;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * The stock ticker's two screens, which are the shopkeeper's side of a counter and the customer's.
 * <p>
 * Neither can be reached by a ticker standing on its own: a ticker needs somebody minding it. Anything
 * sitting on a seat beside it counts, so this shop is staffed by a pig on one - which is what both
 * screens hang off.
 * <p>
 * From there the two are told apart by what is clicked. Clicking the ticker itself is the owner opening
 * the categories they sort their stock into; clicking the keeper is a customer walking up to the counter
 * and asking for something.
 */
@SharedWorld
public class StockKeeperScreenTest {

	private static final int SETTLE_TICKS = 10;

	/** Long enough for the seat and its rider to have reached the client. */
	private static final int SEATING_TICKS = 20;

	@ClientGameTest(screenshot = false)
	@DisplayName("A stock ticker with somebody minding it opens the categories its stock is sorted into")
	void opensTheCategories(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		build(context, singleplayer, server);

		ScreenTesting.rightClickBlock(context, server, ticker());
		ScreenTesting.waitForScreen(context, StockKeeperCategoryScreen.class);
		context.takeScreenshot(shot("stock_keeper_categories"));

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);
	}

	@ClientGameTest(screenshot = false)
	@DisplayName("Walking up to the keeper of a stock ticker opens the counter to order from")
	void opensTheCounter(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		build(context, singleplayer, server);

		// The keeper rather than the block: the same shop, from the other side of the counter.
		ScreenTesting.rightClickEntityAt(context, server, seat());
		ScreenTesting.waitForScreen(context, StockKeeperRequestScreen.class);
		context.takeScreenshot(shot("stock_keeper_counter"));

		ScreenTesting.closeWithEscape(context);
		context.waitTicks(SETTLE_TICKS);
	}

	/** A ticker with a seat beside it and something sitting on the seat to mind the shop. */
	private void build(ClientGameTestContext context, TestSingleplayerContext singleplayer,
		TestServerContext server) {
		singleplayer.getClientLevel()
			.waitForChunksRender();

		ScreenTesting.clearGround(server, ticker(), 5);

		server.runCommand("setblock %d %d %d create:stock_ticker[facing=south]".formatted(ticker().getX(),
			ticker().getY(), ticker().getZ()));
		server.runCommand("setblock %d %d %d create:red_seat".formatted(seat().getX(), seat().getY(),
			seat().getZ()));

		// An empty hand, since a shopping list or a linked item is answered differently.
		server.runCommand("item replace entity @a hotbar.0 with minecraft:air");
		context.runOnClient(client -> client.player.getInventory()
			.setSelectedSlot(0));

		// One is summoned only if the last test has not already left one standing about: clearing the
		// ground takes the seat away but leaves whoever was on it, so it can simply sit back down.
		boolean somebodyAlready = server.computeOnServer(minecraftServer -> !minecraftServer.overworld()
			.getEntitiesOfClass(Mob.class, new AABB(seat()).inflate(4))
			.isEmpty());

		if (!somebodyAlready) {
			server.runCommand("summon minecraft:pig %s %s %s {NoAI:1b}".formatted(seat().getX() + 0.5,
				seat().getY() + 1, seat().getZ() + 0.5));
			context.waitTicks(SETTLE_TICKS);
		}

		// Nothing else about the animal matters - a ticker only asks whether the seat beside it is taken.
		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();

			for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(seat()).inflate(4)))
				SeatBlock.sitDown(level, seat(), mob);
		});
		context.waitTicks(SEATING_TICKS);

		boolean seatTaken = server.computeOnServer(minecraftServer -> minecraftServer.overworld()
			.getEntitiesOfClass(SeatEntity.class, new AABB(seat()).inflate(1))
			.stream()
			.anyMatch(SeatEntity::isVehicle));

		assertTrue(seatTaken, "Nothing sat down on the seat, so the shop has no keeper");
	}

	private BlockPos ticker() {
		return new BlockPos(60, -58, 60);
	}

	/** Beside the ticker, which is as near as a keeper has to be. */
	private BlockPos seat() {
		return ticker().east();
	}

	private static TestScreenshotOptions shot(String name) {
		return TestScreenshotOptions.of(name)
			.withSize(1280, 720);
	}

}
