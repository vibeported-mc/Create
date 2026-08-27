package com.simibubi.create.content.equipment.blueprint;

import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record BlueprintAssignCompleteRecipePacket(Identifier recipeId) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket> STREAM_CODEC = Identifier.STREAM_CODEC.map(
			com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket::new, com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket::recipeId
	);

	@Override
	public void handle(ServerPlayer player) {
		if (player.containerMenu instanceof BlueprintMenu c) {
			RecipeFinder.byKey(ResourceKey.create(Registries.RECIPE, recipeId), player.level())
				.ifPresent(r -> BlueprintItem.assignCompleteRecipe(c.player.level(), c.ghostInventory, r.value()));
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.BLUEPRINT_COMPLETE_RECIPE.getType();
	}
}
