package com.simibubi.create.content.equipment.symmetryWand;

import net.createmod.catnip.api.network.NetworkHelper;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The client half of SymmetryWandItem: opening a screen names client types, and the JVM resolves
 * them when it verifies the class that declares them -- so a common class carrying this could
 * not be loaded on a dedicated server, where the block or item is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class SymmetryWandItemClient {

	public static void openWandGUI(ItemStack wand, InteractionHand hand) {
		ScreenOpener.open(new SymmetryWandScreen(wand, hand));
	}

	private SymmetryWandItemClient() {}
}
