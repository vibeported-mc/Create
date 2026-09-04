package com.simibubi.create.content.equipment.clipboard;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The client half of ClipboardBlock: opening a screen names client types, and the JVM
 * resolves them when it verifies the class that holds them -- so a common class carrying this
 * could not be loaded on a dedicated server, where the block is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class ClipboardBlockClient {

	public static void openScreen(Player player, DataComponentMap components, BlockPos pos) {
		if (Minecraft.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().getSelectedSlot(), components, pos));
	}

	private ClipboardBlockClient() {}
}
