package com.simibubi.create;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.schematics.SchematicProcessor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Minecraft 26.2 registers the processor's codec itself rather than a type object that hands one
 * out, so {@code StructureProcessorType} no longer appears here.
 */
public class AllStructureProcessorTypes {
	private static final DeferredRegister<MapCodec<? extends StructureProcessor>> REGISTER =
		DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Create.ID);

	public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<SchematicProcessor>> SCHEMATIC =
		REGISTER.register("schematic", () -> SchematicProcessor.CODEC);

	@Internal
	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
