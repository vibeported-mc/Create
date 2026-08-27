package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FixLightingCommand {
	static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("fixLighting")
			.requires(Commands.hasPermission(Commands.LEVEL_ALL))
			.executes(ctx -> {
				// NeoForge's experimental light pipeline is gone in 26.2 - the pipeline it used to switch
				// on is the only one left - so all this command can still do is rebuild the world's
				// geometry, which is what actually clears up stale lighting.
				Minecraft.getInstance().levelExtractor.allChanged();

				ctx.getSource()
					.sendSuccess(() -> Component.literal("Rebuilding all chunks."), true);
				return Command.SINGLE_SUCCESS;
			});
	}
}
