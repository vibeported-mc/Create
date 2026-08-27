package com.simibubi.create.foundation.item;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Runs something once, after a transaction is actually kept.
 * <p>
 * Several of Create's handlers do more than move items - they play a sound, wake a machine up, or
 * mark a block entity dirty. Under 26.2's transfer API a transfer may still be rolled back after the
 * handler returns, so those effects cannot happen inline. {@link TransactionContext} has no
 * completion hook of its own; {@link SnapshotJournal#onRootCommit} is the one the API offers, so
 * this is a journal with no state to snapshot, used purely for its commit callback.
 */
public class CommitCallback extends SnapshotJournal<Boolean> {

	private final Runnable onCommit;

	public CommitCallback(Runnable onCommit) {
		this.onCommit = onCommit;
	}

	/**
	 * Arm the callback for this transaction. Calling this more than once within one transaction is
	 * harmless - the journal only records the first snapshot, and the callback still runs once.
	 */
	public void arm(TransactionContext transaction) {
		updateSnapshots(transaction);
	}

	@Override
	protected Boolean createSnapshot() {
		return Boolean.TRUE;
	}

	@Override
	protected void revertToSnapshot(Boolean snapshot) {
	}

	@Override
	protected void onRootCommit(Boolean originalState) {
		onCommit.run();
	}

}
