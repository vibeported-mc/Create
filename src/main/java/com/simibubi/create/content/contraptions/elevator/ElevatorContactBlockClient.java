package com.simibubi.create.content.contraptions.elevator;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.redstone.Orientation;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The client half of ElevatorContactBlock: opening a screen names client types, and the JVM
 * resolves them when it verifies the class that holds them -- so a common class carrying this
 * could not be loaded on a dedicated server, where the block is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class ElevatorContactBlockClient {

	public static void displayScreen(ElevatorContactBlockEntity be, Player player) {
		if (player instanceof LocalPlayer)
			ScreenOpener
				.open(new ElevatorContactScreen(be.getBlockPos(), be.shortName, be.longName, be.doorControls.mode));
	}

	private ElevatorContactBlockClient() {}
}
