package com.simibubi.create.foundation.transfer;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Opening transactions from code that does not know whether it is already inside one.
 */
public class Transactions {

	/**
	 * A transaction nested inside whichever one is already running on this thread, or a root one when
	 * there is none.
	 * <p>
	 * Create's handler helpers stand in for the old API, which had no transactions at all, so they get
	 * called from both sides of that line: sometimes at the top of an operation, and sometimes deep
	 * inside one somebody else opened. A chute inserting a package into a packager is the second case
	 * - the packager unwraps the package and puts its contents in a chest, through these same helpers,
	 * while the chute's own transaction is still open - and opening a root transaction there throws.
	 */
	@SuppressWarnings("deprecation")
	public static Transaction open() {
		return Transaction.open(current());
	}

	/**
	 * The transaction this thread is inside, or null. Handing this to a NeoForge helper as a parent
	 * has the same effect as {@link #open()}.
	 */
	@SuppressWarnings("deprecation")
	public static TransactionContext current() {
		return Transaction.getCurrentOpenedTransaction();
	}

	private Transactions() {
	}

}
