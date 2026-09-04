package com.simibubi.create.foundation.data;

import net.minecraft.client.data.models.model.TextureSlot;

/**
 * The numbered texture slots Create's hand-written models use.
 * <p>
 * Several of Create's models name their textures by number, and a template and its mapping have to
 * hand around the same {@link TextureSlot} instance, which is compared by identity. So they are
 * constants rather than created where they are used.
 * <p>
 * They live here, and not in {@code AllBlocks} where they are read, because {@link TextureSlot} is a
 * client class. A static field of a client type is initialised when its owner is, and {@code
 * AllBlocks} is initialised by every dedicated server -- so holding them there stopped the server
 * from starting at all. Referenced only from inside model-building lambdas, which a server never
 * runs, this class is never loaded there.
 */
public class ModelSlots {

	public static final TextureSlot SLOT_0 = TextureSlot.create("0");
	public static final TextureSlot SLOT_1 = TextureSlot.create("1");
	public static final TextureSlot SLOT_2 = TextureSlot.create("2");
	public static final TextureSlot SLOT_4 = TextureSlot.create("4");
	public static final TextureSlot SLOT_5 = TextureSlot.create("5");

	private ModelSlots() {}
}
