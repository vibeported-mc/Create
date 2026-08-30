package com.simibubi.create.infrastructure.gametest;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.gametest.tests.TestContraptions;
import com.simibubi.create.infrastructure.gametest.tests.TestFluids;
import com.simibubi.create.infrastructure.gametest.tests.TestItems;
import com.simibubi.create.infrastructure.gametest.tests.TestMisc;
import com.simibubi.create.infrastructure.gametest.tests.TestProcessing;
import com.simibubi.create.infrastructure.gametest.tests.TestRegressions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Puts Create's game tests into the game.
 * <p>
 * These are the tests that run on a server with no client at all, each in a structure of its own loaded
 * from a saved schematic. They are registered from code rather than written out as data files: there are
 * seventy of them, they are found by looking over the classes they live on, and a file per test would be
 * one more thing to keep in step with the methods themselves.
 */
@EventBusSubscriber
public class CreateGameTests {

	private static final Class<?>[] testHolders =
		{ TestContraptions.class, TestFluids.class, TestItems.class, TestMisc.class, TestProcessing.class,
			TestRegressions.class };

	/**
	 * Every test instance must name a kind of test it is, and the kinds live in a registry of their own.
	 */
	private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
		DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Create.ID);

	private static final DeferredHolder<MapCodec<? extends GameTestInstance>, MapCodec<CreateTestInstance>> TEST_TYPE =
		TEST_TYPES.register("test", () -> CreateTestInstance.CODEC);

	public static void register(IEventBus modEventBus) {
		TEST_TYPES.register(modEventBus);
	}

	@SubscribeEvent
	public static void registerTests(RegisterGameTestsEvent event) {
		// Nothing is asked of the world these tests run in beyond what the structure itself brings, so the
		// environment they share sets nothing up and takes nothing down.
		Holder<TestEnvironmentDefinition<?>> environment =
			event.registerEnvironment(Create.asResource("default"));

		// -PserverGameTestSelect=<name> narrows a run to one test, which is how one is looked at on its
		// own. The name given is matched whole rather than as a piece of one, so that asking for
		// brass_mixing does not also bring brass_mixing2 along and put two structures in the world.
		String only = System.getProperty("create.gametest.server.select", "");

		for (CreateTestFunction.Found test : CreateTestFunction.getTestsFrom(environment, testHolders)) {
			String path = test.id()
				.getPath();

			if (only.isEmpty() || path.equals(only) || path.endsWith("/" + only))
				event.registerTest(test.id(), test.instance());
		}
	}

}
