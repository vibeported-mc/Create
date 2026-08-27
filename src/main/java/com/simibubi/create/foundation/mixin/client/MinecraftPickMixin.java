package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.block.BigOutlines;

import net.minecraft.client.Minecraft;

/**
 * Create picks its own oversized outlines right after vanilla picks a block.
 * <p>
 * 26.2 moved {@code pick} off the game renderer and onto {@link Minecraft} itself.
 */
@Mixin(Minecraft.class)
public class MinecraftPickMixin {
	@Inject(method = "pick(F)V", at = @At("TAIL"))
	private void create$bigShapePick(CallbackInfo ci) {
		BigOutlines.pick();
		TrackBlockOutline.pickCurves();
	}
}
