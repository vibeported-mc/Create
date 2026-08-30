package com.simibubi.create.infrastructure.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a game test, and says which structure it should be run in.
 * <p>
 * Create's own, because the game no longer has one. Tests used to be found by an annotation the game
 * provided and turned into a {@code TestFunction}; they are now registry objects built in code or read
 * from data files, and the annotation went with the old way. Keeping one here means the tests themselves
 * read as they always did, and only the finding of them had to be rewritten.
 *
 * @see CreateTestFunction
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GameTest {

	/**
	 * The structure the test is run in, named without its folder or extension.
	 * <p>
	 * Where it is looked for comes from the {@link GameTestGroup} on the class the method is in.
	 */
	String template();

	/**
	 * How long the test may run for before it is counted as failed.
	 */
	int timeoutTicks() default 100;

	/**
	 * How long to let the structure settle before the test itself begins.
	 */
	int setupTicks() default 0;

	/**
	 * Whether a failure here fails the whole run.
	 */
	boolean required() default true;

	/**
	 * Quarter turns to place the structure by, for tests that care which way they face.
	 */
	int rotationSteps() default 0;

	/**
	 * How many times to run the test before giving up on it.
	 */
	int attempts() default 1;

	/**
	 * How many of those attempts have to pass.
	 */
	int requiredSuccesses() default 1;

}
