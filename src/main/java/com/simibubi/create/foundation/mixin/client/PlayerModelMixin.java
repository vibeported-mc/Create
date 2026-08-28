package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * 26.2 poses models from a render state rather than from the entity, so this hangs off
 * {@link PlayerModel} - the only humanoid model that can belong to a player - and looks the player
 * back up by the entity id the state carries.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
	private void create$afterSetupAnim(AvatarRenderState state, CallbackInfo callbackInfo) {
		AbstractClientPlayer player = create$playerOf(state);
		if (player == null)
			return;

		PlayerSkyhookRenderer.afterSetupAnim(player, (HumanoidModel<?>) (Object) this);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"))
	private void create$beforeSetupAnim(AvatarRenderState state, CallbackInfo callbackInfo) {
		AbstractClientPlayer player = create$playerOf(state);
		if (player == null)
			return;

		PlayerSkyhookRenderer.beforeSetupAnim(player, (HumanoidModel<?>) (Object) this);
	}

	private static AbstractClientPlayer create$playerOf(AvatarRenderState state) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return null;
		return level.getEntity(state.id) instanceof AbstractClientPlayer player ? player : null;
	}
}
