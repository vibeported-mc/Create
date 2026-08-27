package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.Font;

/**
 * A font is built from a glyph provider, which it keeps to itself; Create wraps a font to suppress
 * its shadow and needs the same one.
 */
@Mixin(Font.class)
public interface FontAccessor {
	@Accessor("provider")
	Font.Provider create$getProvider();
}
