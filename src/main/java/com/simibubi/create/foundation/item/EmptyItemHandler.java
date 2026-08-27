package com.simibubi.create.foundation.item;

/**
 * A handler with no slots, standing in wherever a capability has not resolved.
 * <p>
 * NeoForge's own empty handler is gone in 26.2; a zero-size handler behaves the same, rejecting every
 * insertion and holding nothing.
 */
public class EmptyItemHandler extends ItemStackHandler {
	public static final EmptyItemHandler INSTANCE = new EmptyItemHandler();

	private EmptyItemHandler() {
		super(0);
	}
}
