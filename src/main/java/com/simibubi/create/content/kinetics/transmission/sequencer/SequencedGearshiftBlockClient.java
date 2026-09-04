package com.simibubi.create.content.kinetics.transmission.sequencer;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The client half of SequencedGearshiftBlock: opening a screen names client types, and the JVM
 * resolves them when it verifies the class that holds them -- so a common class carrying this
 * could not be loaded on a dedicated server, where the block is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class SequencedGearshiftBlockClient {

	public static void displayScreen(SequencedGearshiftBlockEntity be, Player player) {
		if (player instanceof LocalPlayer)
			ScreenOpener.open(new SequencedGearshiftScreen(be));
	}

	private SequencedGearshiftBlockClient() {}
}
