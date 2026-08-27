package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.context.CommandContext;
import org.apache.commons.lang3.mutable.MutableInt;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceInCommandBlocksCommand {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("replaceInCommandBlocks")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.argument("begin", BlockPosArgument.blockPos())
				.then(Commands.argument("end", BlockPosArgument.blockPos())
					.then(Commands.argument("toReplace", StringArgumentType.string())
						.then(Commands.argument("replaceWith", StringArgumentType.string())
							.executes(ctx -> {
								doReplace(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "begin"),
									BlockPosArgument.getLoadedBlockPos(ctx, "end"),
									getStringOrEmpty(ctx, "toReplace"),
									getStringOrEmpty(ctx, "replaceWith"));
								return 1;
							})))));

	}

	private static void doReplace(CommandSourceStack source, BlockPos from, BlockPos to, String toReplace,
								  String replaceWith) {
		ServerLevel world = source.getLevel();
		MutableInt blocks = new MutableInt(0);
		BlockPos.betweenClosedStream(from, to)
			.forEach(pos -> {
				BlockState blockState = world.getBlockState(pos);
				if (!(blockState.getBlock() instanceof CommandBlock))
					return;
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (!(blockEntity instanceof CommandBlockEntity cb))
					return;
				BaseCommandBlock commandBlockLogic = cb.getCommandBlock();
				String command = commandBlockLogic.getCommand();
				if (command.indexOf(toReplace) != -1)
					blocks.increment();
				commandBlockLogic.setCommand(command.replaceAll(toReplace, replaceWith));
				cb.setChanged();
				world.sendBlockUpdated(pos, blockState, blockState, 2);
			});
		int intValue = blocks.intValue();
		if (intValue == 0) {
			source.sendSuccess(() -> {
				return Component.literal("Couldn't find \"" + toReplace + "\" anywhere.");
			}, true);
			return;
		}
		source.sendSuccess(() -> {
			return Component.literal("Replaced occurrences in " + intValue + " blocks.");
		}, true);
	}


	/**
	 * Brigadier's defaulting getter is gone in 26.2; an argument that was never given simply is not in
	 * the context.
	 */
	private static String getStringOrEmpty(CommandContext<CommandSourceStack> ctx, String name) {
		try {
			return StringArgumentType.getString(ctx, name);
		} catch (IllegalArgumentException e) {
			return "";
		}
	}

}
