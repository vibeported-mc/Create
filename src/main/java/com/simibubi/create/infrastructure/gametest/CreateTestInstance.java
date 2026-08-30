package com.simibubi.create.infrastructure.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * One of Create's game tests, as the game now wants them: an object in a registry rather than a method
 * found by an annotation.
 * <p>
 * All this holds is the method to call and what the game needs to set the test up. The method itself is
 * still an ordinary static one on a class in {@code tests}, which is what keeps the tests readable.
 */
public class CreateTestInstance extends GameTestInstance {

	/**
	 * Only here because every test instance must name a codec.
	 * <p>
	 * These tests are built in code and put into the registry as the game starts, never written to a data
	 * file and read back, so there is nothing for a codec to do - and a method cannot be written down in
	 * any case. Reading one out of a data file is a mistake worth saying so about.
	 */
	public static final MapCodec<CreateTestInstance> CODEC = MapCodec.unit(() -> {
		throw new UnsupportedOperationException(
			"Create's game tests are registered in code, so there is none to read from a data file");
	});

	private final String name;

	private final Consumer<CreateGameTestHelper> body;

	public CreateTestInstance(String name, Consumer<CreateGameTestHelper> body,
		TestData<Holder<TestEnvironmentDefinition<?>>> data) {
		super(data);

		this.name = name;
		this.body = body;
	}

	@Override
	public void run(GameTestHelper helper) {
		body.accept(CreateGameTestHelper.of(helper));
	}

	@Override
	public MapCodec<? extends GameTestInstance> codec() {
		return CODEC;
	}

	@Override
	protected MutableComponent typeDescription() {
		return Component.literal("Create test");
	}

	@Override
	public Component describe() {
		return Component.literal(name);
	}

}
