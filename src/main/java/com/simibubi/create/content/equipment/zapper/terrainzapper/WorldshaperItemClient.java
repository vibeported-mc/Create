package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.zapper.PlacementPatterns;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The client half of WorldshaperItem: opening a screen names client types, and the JVM resolves
 * them when it verifies the class that declares them -- so a common class carrying this could
 * not be loaded on a dedicated server, where the block or item is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class WorldshaperItemClient {

	public static void openHandgunGUI(ItemStack item, InteractionHand hand) {
		ScreenOpener.open(new WorldshaperScreen(item, hand));
	}

	private WorldshaperItemClient() {}
}
