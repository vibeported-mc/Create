package com.simibubi.create.content.redstone.displayLink;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.RedstonePowerDisplaySource;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The client half of DisplayLinkBlock: opening a screen names client types, and the JVM
 * resolves them when it verifies the class that holds them -- so a common class carrying this
 * could not be loaded on a dedicated server, where the block is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class DisplayLinkBlockClient {

	public static void displayScreen(DisplayLinkBlockEntity be, Player player) {
		if (!(player instanceof LocalPlayer))
			return;
		if (be.targetOffset.equals(BlockPos.ZERO)) {
			player.sendOverlayMessage(CreateLang.translateDirect("display_link.invalid"));
			return;
		}
		ScreenOpener.open(new DisplayLinkScreen(be));
	}

	private DisplayLinkBlockClient() {}
}
