package com.simibubi.create.foundation.utility;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Bridges Minecraft 26.2's ValueIO to the {@link CompoundTag} shape Create writes.
 * <p>
 * Entities and block entities save through {@link ValueOutput} and {@link ValueInput} now. Create's
 * serialisation is hand-written against CompoundTag throughout - often through helpers shared with
 * packets and contraption storage, which still speak tags - so these move a whole tag across the
 * boundary in one piece rather than rewriting every body. The stored shape is identical either way,
 * since ValueIO writes the same keys at the same level.
 */
public class NbtValueIO {

	@SuppressWarnings("deprecation")
	private static final MapCodec<CompoundTag> WHOLE = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

	/**
	 * Write a tag's entries at the root of the output.
	 */
	public static void store(ValueOutput output, CompoundTag tag) {
		output.store(tag);
	}

	/**
	 * Read everything at the root of the input back as a tag.
	 */
	public static CompoundTag read(ValueInput input) {
		return input.read(WHOLE)
			.orElseGet(CompoundTag::new);
	}

	/**
	 * A fresh tag holding whatever the writer stores.
	 * <p>
	 * For the small, registry-free pieces Create keeps as nested tags - animation state and the like -
	 * which now write themselves through {@link ValueOutput}.
	 */
	public static CompoundTag toTag(Consumer<ValueOutput> writer) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, RegistryAccess.EMPTY);
		writer.accept(output);
		return output.buildResult();
	}

	/**
	 * A view of a tag that can be handed to something reading through {@link ValueInput}.
	 */
	public static ValueInput fromTag(CompoundTag tag) {
		return TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag);
	}

	private NbtValueIO() {
	}

}
