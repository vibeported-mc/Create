package com.simibubi.create.infrastructure.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
	 * The body of a test that came over the wire instead of being registered here.
	 */
	private static final Consumer<CreateGameTestHelper> RECEIVED_NOT_REGISTERED = helper -> {
		throw new UnsupportedOperationException(
			"This test was read from a registry rather than registered in code, so the method it should"
				+ " call did not come with it. Run Create's game tests on the side that registers them.");
	};

	/**
	 * What a test instance carries over the wire, which is everything about it except the method to call.
	 * <p>
	 * These tests are built in code rather than written out as data files, so it is tempting to think a
	 * codec is never needed. It is: {@code minecraft:test_instance} is one of the registries a server
	 * sends to a client as it joins, so every registered test is encoded and decoded on every connection.
	 * A codec that refuses to decode takes the whole registry down with it, and the client is disconnected
	 * with a protocol error before it ever reaches the world.
	 * <p>
	 * The method itself cannot be written down, so a test that arrives this way keeps everything the game
	 * needs to list it and says so if anything tries to run it. Only the side that registered the test in
	 * code can actually run it, which is the side that has the method.
	 */
	public static final MapCodec<CreateTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
		.group(Codec.STRING.fieldOf("name")
			.forGetter(test -> test.name),
			TestData.CODEC.forGetter(CreateTestInstance::info))
		.apply(instance, (name, data) -> new CreateTestInstance(name, RECEIVED_NOT_REGISTERED, data)));


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
