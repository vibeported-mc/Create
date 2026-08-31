package com.simibubi.create.infrastructure.gametest.tests;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTest;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fluid that is moved from one place to another should still all be there afterwards.
 * <p>
 * These borrow a structure for its floor and build what they need on top of it, so that the same
 * scene can be written the same way on either side of the port.
 */
@GameTestGroup(path = "fluids")
public class TestTransferFluids {

	/**
	 * Two tanks holding fluid, joined into one by filling the gap between them. A tank being taken into
	 * a larger one hands its contents over, and what it was holding has to arrive.
	 */
	@GameTest(template = "hose_pulley_transfer", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void tankMergeConservesFluid(CreateGameTestHelper helper) {
		clearArena(helper);

		BlockPos lower = new BlockPos(2, 2, 2);
		BlockPos middle = new BlockPos(2, 3, 2);
		BlockPos upper = new BlockPos(2, 4, 2);

		helper.setBlock(lower, AllBlocks.FLUID_TANK.getDefaultState());
		helper.setBlock(upper, AllBlocks.FLUID_TANK.getDefaultState());

		helper.runAtTickTime(2, () -> {
			fill(helper, lower, 500);
			fill(helper, upper, 500);
		});

		// Only now do they become one tank.
		helper.runAtTickTime(6, () -> helper.setBlock(middle, AllBlocks.FLUID_TANK.getDefaultState()));

		helper.succeedWhen(() -> {
			helper.assertSecondsPassed(1);
			// Merged, so every part answers for the whole: ask one of them, not all three.
			long held = helper.getTankContents(lower)
				.getAmount();
			if (held != 1000)
				helper.fail("Merged tank holds " + held + "mB of the 1000mB put into it");
		});
	}

	/**
	 * The same tank taken apart again. What the remaining tank can hold, it should keep.
	 */
	@GameTest(template = "hose_pulley_transfer", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void tankSplitConservesFluid(CreateGameTestHelper helper) {
		clearArena(helper);

		BlockPos lower = new BlockPos(2, 2, 2);
		BlockPos upper = new BlockPos(2, 3, 2);

		helper.setBlock(lower, AllBlocks.FLUID_TANK.getDefaultState());
		helper.setBlock(upper, AllBlocks.FLUID_TANK.getDefaultState());

		// Less than one tank holds, so breaking the other one costs nothing.
		helper.runAtTickTime(4, () -> fill(helper, lower, 3000));
		helper.runAtTickTime(8, () -> helper.setBlock(upper, Blocks.AIR));

		helper.succeedWhen(() -> {
			helper.assertSecondsPassed(1);
			long held = helper.getTankContents(lower)
				.getAmount();
			if (held != 3000)
				helper.fail("Tank kept " + held + "mB of the 3000mB it held before the one above it went");
		});
	}

	/**
	 * What an insertion says it could not take has to match what it left behind.
	 * <p>
	 * Most of the places that move items call this and act on the number it returns - or, in a few
	 * places, do not, and then whatever it could not take is gone. Either way the number has to be
	 * right, so it is checked directly against a container with a known amount of room.
	 */
	@GameTest(template = "hose_pulley_transfer", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void insertStackedReportsRemainder(CreateGameTestHelper helper) {
		clearArena(helper);

		BlockPos chestPos = new BlockPos(2, 2, 2);
		helper.setBlock(chestPos, Blocks.CHEST);

		helper.runAtTickTime(2, () -> {
			net.minecraft.world.Container chest = (net.minecraft.world.Container) helper.getLevel()
				.getBlockEntity(helper.absolutePos(chestPos));

			// Full but for four diamonds' worth of room in the first slot.
			for (int slot = 0; slot < chest.getContainerSize(); slot++)
				chest.setItem(slot, new net.minecraft.world.item.ItemStack(
					net.minecraft.world.item.Items.DIAMOND, 64));
			chest.setItem(0, new net.minecraft.world.item.ItemStack(
				net.minecraft.world.item.Items.DIAMOND, 60));

			int before = count(chest);

			net.minecraft.world.item.ItemStack offered =
				new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 10);
			int accepted;

			try (Transaction transaction = Transaction.openRoot()) {
				accepted = ResourceHandlerUtil.insertStacking(helper.itemStorageAt(chestPos),
					ItemResource.of(offered), offered.getCount(), transaction);
				transaction.commit();
			}

			net.minecraft.world.item.ItemStack remainder = offered.copyWithCount(offered.getCount() - accepted);

			int after = count(chest);

			if (after - before != 4)
				helper.fail("Chest took " + (after - before) + " of the 10 offered, with room for 4");
			if (remainder.getCount() != 6)
				helper.fail("Insertion handed back " + remainder.getCount() + " of the 10 offered, not 6");

			helper.succeed();
		});
	}

	private static int count(net.minecraft.world.Container chest) {
		int total = 0;
		for (int slot = 0; slot < chest.getContainerSize(); slot++)
			total += chest.getItem(slot)
				.getCount();
		return total;
	}

	/**
	 * What a fill says it took has to match what the tank gained.
	 * <p>
	 * The same question as the insertion one, for fluid: several places fill a tank and ignore the
	 * answer, so anything the tank would not take is gone. The answer itself has to be right.
	 */
	@GameTest(template = "hose_pulley_transfer", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
	public static void fillReportsAcceptedAmount(CreateGameTestHelper helper) {
		clearArena(helper);

		BlockPos tank = new BlockPos(2, 2, 2);
		helper.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());

		helper.runAtTickTime(4, () -> {
			long capacity = helper.getTankCapacity(tank);
			fill(helper, tank, (int) capacity - 200);

			long before = helper.getTankContents(tank)
				.getAmount();
			int accepted;
			try (Transaction transaction = Transaction.openRoot()) {
				int transferred = helper.fluidStorageAt(tank).insert(FluidResource.of(new FluidStack(Fluids.WATER, 500)), new FluidStack(Fluids.WATER, 500).getAmount(), transaction);
				accepted = transferred;
				transaction.commit();
			}
			long after = helper.getTankContents(tank)
				.getAmount();

			if (after - before != 200)
				helper.fail("Tank gained " + (after - before) + "mB of the 500mB offered, with room for 200");
			if (accepted != 200)
				helper.fail("Fill reported taking " + accepted + "mB of the 500mB offered, not 200");

			helper.succeed();
		});
	}

	/**
	 * A basin recipe that asks for fluid has to take that fluid out of the basin.
	 * <p>
	 * A recipe's fluid ingredient is read out of the basin, and the amount it uses is then taken off
	 * that reading. If the reading is a copy of what the basin holds rather than the basin's own, the
	 * subtraction lands on nothing and the basin keeps its fluid while still handing over the result -
	 * every mixing, brewing and compacting recipe becomes a fluid duplicator. The amount is checked
	 * exactly rather than just "less than before", since a recipe taking the wrong quantity is the same
	 * bug wearing a different number.
	 */
	@GameTest(template = "hose_pulley_transfer", timeoutTicks = CreateGameTestHelper.THIRTY_SECONDS)
	public static void basinConsumesItsFluid(CreateGameTestHelper helper) {
		clearArena(helper);

		BlockPos basin = new BlockPos(2, 2, 2);
		BlockPos mixer = new BlockPos(2, 4, 2);
		BlockPos cog = new BlockPos(3, 4, 2);
		BlockPos motor = new BlockPos(3, 5, 2);

		// Basin, a gap for the mixer's pole, the mixer, and a cogwheel beside it being turned. The mixer
		// is itself a cogwheel and takes no shaft, so power reaches it by meshing with the one next to
		// it - the same way the hand built mixing structures in this suite are wired.
		helper.setBlock(basin, AllBlocks.BASIN.getDefaultState());
		helper.setBlock(mixer, AllBlocks.MECHANICAL_MIXER.getDefaultState());
		helper.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState()
			.setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
		helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
			.setValue(DirectionalKineticBlock.FACING, Direction.DOWN));

		helper.runAtTickTime(2, () -> {
			// Faster than the mixer's minimum, so the only thing being timed is the recipe.
			CreativeMotorBlockEntity motorBe =
				helper.getBlockEntity(AllBlockEntityTypes.MOTOR.get(), motor);
			motorBe.generatedSpeed.setValue(64);

			// Twice what the recipe asks for, so a basin that consumed nothing and a basin that
			// consumed everything both read differently from a basin that consumed the right amount.
			try (Transaction transaction = Transaction.openRoot()) {
				helper.fluidStorageAt(basin).insert(FluidResource.of(new FluidStack(Fluids.WATER, 500)), new FluidStack(Fluids.WATER, 500).getAmount(), transaction);
				transaction.commit();
			}
			try (Transaction transaction = Transaction.openRoot()) {
				ResourceHandlerUtil.insertStacking(helper.itemStorageAt(basin), ItemResource.of(new ItemStack(Items.DIRT)), new ItemStack(Items.DIRT).getCount(), transaction);
				transaction.commit();
			}
		});

		// Dirt and 250mB of water make mud.
		helper.succeedWhen(() -> {
			helper.assertContainerContains(basin, Items.MUD);

			long held = helper.getTankContents(basin)
				.getAmount();
			if (held != 250)
				helper.fail("Basin holds " + held + "mB after a recipe that should have taken 250mB of its 500mB");
		});
	}

	private static void fill(CreateGameTestHelper helper, BlockPos tank, int amount) {
		try (Transaction transaction = Transaction.openRoot()) {
			helper.fluidStorageAt(tank).insert(FluidResource.of(new FluidStack(Fluids.WATER, amount)), new FluidStack(Fluids.WATER, amount).getAmount(), transaction);
			transaction.commit();
		}
	}

	/**
	 * The structure these borrow is only wanted for its floor.
	 */
	private static void clearArena(CreateGameTestHelper helper) {
		BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(12, 6, 6))
			.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
	}
}
