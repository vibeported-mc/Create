package com.simibubi.create.infrastructure.gametest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

/**
 * Finds Create's game tests and builds them into the objects the game now runs.
 * <p>
 * A test is still written as a static method with a {@link GameTest} on it, in a class with a
 * {@link GameTestGroup} saying where its structures live. What has changed is what becomes of them: the
 * game used to collect such methods itself and wrap each in a {@code TestFunction}, and now expects a
 * {@link CreateTestInstance} put into a registry. That collecting is what this does.
 */
public class CreateTestFunction {

	/** One test, ready to be put into the registry under the name it will be run by. */
	public record Found(Identifier id, CreateTestInstance instance) {
	}

	/**
	 * Every test on the given classes, in a settled order.
	 * <p>
	 * The order is only so that a run reads the same way twice; the game makes no promises about it.
	 */
	public static List<Found> getTestsFrom(Holder<TestEnvironmentDefinition<?>> environment,
		Class<?>... classes) {
		List<Found> found = new ArrayList<>();

		for (Class<?> owner : classes)
			for (Method method : owner.getDeclaredMethods()) {
				Found test = of(environment, method);

				if (test != null)
					found.add(test);
			}

		found.sort(Comparator.comparing(test -> test.id()
			.toString()));

		return found;
	}

	@Nullable
	private static Found of(Holder<TestEnvironmentDefinition<?>> environment, Method method) {
		GameTest gt = method.getAnnotation(GameTest.class);

		if (gt == null) // skip non-test methods
			return null;

		Class<?> owner = method.getDeclaringClass();
		GameTestGroup group = owner.getAnnotation(GameTestGroup.class);
		String simpleName = owner.getSimpleName() + '.' + method.getName();
		validateTestMethod(method, gt, owner, group, simpleName);

		// Structures are looked for the way the game looks for any of them: data/<namespace>/structure/,
		// and then the folder this class's group names.
		Identifier structure = Identifier.fromNamespaceAndPath(group.namespace(),
			"gametest/" + group.path() + "/" + gt.template());
		Rotation rotation = StructureUtils.getRotationForRotationSteps(gt.rotationSteps());

		TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, structure,
			gt.timeoutTicks(), gt.setupTicks(), gt.required(), rotation, false, gt.attempts(),
			gt.requiredSuccesses(), false, 0);

		Identifier id = Identifier.fromNamespaceAndPath(group.namespace(),
			asPath(owner.getSimpleName()) + "/" + asPath(method.getName()));

		return new Found(id, new CreateTestInstance(simpleName, asConsumer(method), data));
	}

	/**
	 * A java name as a name the game will accept.
	 * <p>
	 * Only lower case, digits and a few marks are allowed in one, so the humps that separate the words of
	 * a method name become underscores.
	 */
	private static String asPath(String javaName) {
		return javaName.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
			.toLowerCase(Locale.ROOT);
	}

	private static void validateTestMethod(Method method, GameTest gt, Class<?> owner, GameTestGroup group,
		String simpleName) {
		if (gt.template()
			.isEmpty())
			throw new IllegalArgumentException(simpleName + " must provide a template structure");

		if (!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException(simpleName + " must be static");

		if (method.getReturnType() != void.class)
			throw new IllegalArgumentException(simpleName + " must return void");

		if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != CreateGameTestHelper.class)
			throw new IllegalArgumentException(simpleName + " must take 1 parameter of type CreateGameTestHelper");

		if (group == null)
			throw new IllegalArgumentException(owner.getName() + " must be annotated with @GameTestGroup");
	}

	private static Consumer<CreateGameTestHelper> asConsumer(Method method) {
		return helper -> {
			try {
				method.invoke(null, helper);
			} catch (IllegalAccessException | InvocationTargetException e) {
				if (e instanceof InvocationTargetException wrapped
					&& wrapped.getCause() instanceof RuntimeException thrown)
					throw thrown; // a failed assertion is meant to reach the runner as it was thrown

				throw new RuntimeException(e);
			}
		};
	}

}
