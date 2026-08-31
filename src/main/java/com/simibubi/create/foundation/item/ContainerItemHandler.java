package com.simibubi.create.foundation.item;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.transfer.Transactions;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A container seen as a handler whose slots can also be written directly.
 * <p>
 * 26.2's {@link VanillaContainerWrapper} only offers the transactional half of the API. Create hands
 * containers to code that also overwrites slots outright - mounted storage unmounting itself, for one
 * - so the direct write goes to the container, which is what the wrapper reads from anyway.
 */
public class ContainerItemHandler implements ModifiableItemHandler {

	private final Container container;
	private final ResourceHandler<ItemResource> wrapped;
	private final SnapshotJournal<List<ItemStack>> directWrites = new SnapshotJournal<>() {

		@Override
		protected List<ItemStack> createSnapshot() {
			List<ItemStack> contents = new ArrayList<>(container.getContainerSize());

			for (int slot = 0; slot < container.getContainerSize(); slot++)
				contents.add(container.getItem(slot)
					.copy());

			return contents;
		}

		@Override
		protected void revertToSnapshot(List<ItemStack> snapshot) {
			for (int slot = 0; slot < snapshot.size(); slot++)
				container.setItem(slot, snapshot.get(slot));

			container.setChanged();
		}
	};

	/**
	 * The handler if its slots can already be written, or the block's own container seen that way.
	 * <p>
	 * 1.21.1 handed out an {@code InvWrapper} for a plain container, which was modifiable, so a block
	 * that is only a container could be mounted on a contraption and written back on disassembly. Its
	 * replacement is transactional only, so the container behind it is wrapped instead. That is the
	 * whole inventory, without whatever a sided view would have hidden - which is what mounting wants,
	 * since it carries the block's contents off and has to put all of them back.
	 */
	@Nullable
	public static ModifiableItemHandler writable(@Nullable ResourceHandler<ItemResource> handler,
		@Nullable BlockEntity be) {

		if (handler instanceof ModifiableItemHandler modifiable)
			return modifiable;

		if (be instanceof Container container
			&& (handler == null || handler.size() == container.getContainerSize()))
			return new ContainerItemHandler(container);

		return null;
	}

	public ContainerItemHandler(Container container) {
		this.container = container;
		this.wrapped = VanillaContainerWrapper.of(container);
	}

	/**
	 * Writes a slot outright, and takes back the write if the transaction it is inside is thrown away.
	 * <p>
	 * The transactional half of this handler is looked after by what it wraps, but this half writes to
	 * the container directly, and a write that outlives a rolled back transaction is a write out of
	 * nowhere. So the container's contents are noted down before the first such write of a transaction
	 * and put back if that transaction does not commit. Called with no transaction running - which is
	 * how mounting and unmounting use it - there is nothing to note and it simply writes.
	 */
	@Override
	public void set(int index, ItemResource resource, int amount) {
		TransactionContext transaction = Transactions.current();

		if (transaction != null)
			directWrites.updateSnapshots(transaction);

		container.setItem(index, resource.toStack(amount));
		container.setChanged();
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public ItemResource getResource(int index) {
		return wrapped.getResource(index);
	}

	@Override
	public long getAmountAsLong(int index) {
		return wrapped.getAmountAsLong(index);
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		return wrapped.getCapacityAsLong(index, resource);
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return wrapped.isValid(index, resource);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.insert(index, resource, amount, transaction);
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.insert(resource, amount, transaction);
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.extract(index, resource, amount, transaction);
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		return wrapped.extract(resource, amount, transaction);
	}

}
