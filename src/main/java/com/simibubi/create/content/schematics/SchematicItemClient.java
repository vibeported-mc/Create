package com.simibubi.create.content.schematics;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The client half of SchematicItem: opening a screen names client types, and the JVM resolves
 * them when it verifies the class that declares them -- so a common class carrying this could
 * not be loaded on a dedicated server, where the block or item is registered.
 *
 * Until 26.2 the method carried {@code @OnlyIn(Dist.CLIENT)} and was stripped from the server
 * jar. NeoForge no longer strips annotated members.
 */
public class SchematicItemClient {

	public static void displayBlueprintScreen() {
		ScreenOpener.open(new SchematicEditScreen());
	}

	private SchematicItemClient() {}
}
