package com.simibubi.create.content.trains.station;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import com.simibubi.create.foundation.block.EntityRestingOnBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The client half of StationBlock: opening a screen names client types, and the JVM resolves
 * them when it verifies the class that declares them -- so a common class carrying this could
 * not be loaded on a dedicated server, where the block or item is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class StationBlockClient {

	public static void displayScreen(StationBlock block, StationBlockEntity be, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		GlobalStation station = be.getStation();
		BlockState blockState = be.getBlockState();
		if (station == null || blockState == null)
			return;
		boolean assembling = blockState.getBlock() == block && blockState.getValue(StationBlock.ASSEMBLING);
		ScreenOpener.open(assembling ? new AssemblyScreen(be, station) : new StationScreen(be, station));
	}

	private StationBlockClient() {}
}
