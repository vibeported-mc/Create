package com.simibubi.create.content.fluids;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.utility.NbtValueIO;
import com.simibubi.create.AllFluids;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.mixin.accessor.FlowingFluidAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.math.BlockFace;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidStack;
public class OpenEndedPipe extends FlowSource {

	private Level world;
	private BlockPos pos;
	private AABB aoe;

	private OpenEndFluidHandler fluidHandler;
	private BlockPos outputPos;
	private boolean wasPulling;

	private final ICapabilityProvider<ResourceHandler<FluidResource>> fluidHandlerProvider = ICapabilityProvider.of(() -> fluidHandler);

	public OpenEndedPipe(BlockFace face) {
		super(face);
		fluidHandler = new OpenEndFluidHandler();
		outputPos = face.getConnectedPos();
		pos = face.getPos();
		aoe = new AABB(outputPos).expandTowards(0, -1, 0);
		if (face.getFace() == Direction.DOWN)
			aoe = aoe.expandTowards(0, -1, 0);
	}

	public Level getWorld() {
		return world;
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockPos getOutputPos() {
		return outputPos;
	}

	public AABB getAOE() {
		return aoe;
	}

	@Override
	public void manageSource(Level world, BlockEntity networkBE) {
		this.world = world;
	}

	@Override
	@Nullable
	public ICapabilityProvider<ResourceHandler<FluidResource>> provideHandler() {
		return fluidHandlerProvider;
	}

	@Override
	public boolean isEndpoint() {
		return true;
	}

	public CompoundTag serializeNBT(HolderLookup.Provider registries) {
		CompoundTag compound = new CompoundTag();
		compound.merge(NbtValueIO.serialize(fluidHandler, registries));
		compound.putBoolean("Pulling", wasPulling);
		compound.store("Location", BlockFace.CODEC, location);
		return compound;
	}

	public static OpenEndedPipe fromNBT(CompoundTag compound, HolderLookup.Provider registries, BlockPos blockEntityPos) {
		BlockFace stored = compound.read("Location", BlockFace.CODEC)
			.orElse(new BlockFace(blockEntityPos, Direction.UP));
		OpenEndedPipe oep = new OpenEndedPipe(new BlockFace(blockEntityPos, stored.getFace()));

		NbtValueIO.deserialize(oep.fluidHandler, compound, registries);
		oep.wasPulling = compound.getBooleanOr("Pulling", false);
		return oep;
	}

	private FluidStack removeFluidFromSpace(boolean simulate) {
		FluidStack empty = FluidStack.EMPTY;
		if (world == null)
			return empty;
		if (!world.isLoaded(outputPos))
			return empty;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		FluidStack drainBlock = VanillaFluidTargets.drainBlock(world, outputPos, state, simulate);
		if (!drainBlock.isEmpty()) {
			if (!simulate && state.hasProperty(BlockStateProperties.LEVEL_HONEY)
				&& AllFluids.HONEY.is(drainBlock.getFluid()))
				AdvancementBehaviour.tryAward(world, pos, AllAdvancements.HONEY_DRAIN);
			return drainBlock;
		}

		if (!waterlog && !state.canBeReplaced())
			return empty;
		if (fluidState.isEmpty() || !fluidState.isSource())
			return empty;

		FluidStack stack = new FluidStack(fluidState.getType(), 1000);

		if (simulate)
			return stack;

		if (FluidHelper.isWater(stack.getFluid()))
			AdvancementBehaviour.tryAward(world, pos, AllAdvancements.WATER_SUPPLY);

		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, false), Block.UPDATE_ALL);
			world.scheduleTick(outputPos, Fluids.WATER, 1);
		} else {
			var newState = fluidState.createLegacyBlock()
				.setValue(LiquidBlock.LEVEL, 14);

			var newFluidState = newState.getFluidState();

			if (newFluidState.getType() instanceof FlowingFluidAccessor flowing) {
				var potentiallyFilled = flowing.create$getNewLiquid(world, outputPos, newState);

				// Check if we'd immediately become the same fluid again.
				if (potentiallyFilled.equals(fluidState)) {
					// If so, no need to update the block state.
					return stack;
				}
			}

			world.setBlock(outputPos, newState, Block.UPDATE_ALL);
		}

		return stack;
	}

	private boolean provideFluidToSpace(FluidStack fluid, boolean simulate) {
		if (world == null)
			return false;
		if (!world.isLoaded(outputPos))
			return false;

		BlockState state = world.getBlockState(outputPos);
		FluidState fluidState = state.getFluidState();
		boolean waterlog = state.hasProperty(WATERLOGGED);

		if (!waterlog && !state.canBeReplaced())
			return false;
		if (fluid.isEmpty())
			return false;
		if (!(fluid.getFluid() instanceof FlowingFluid))
			return false;
		if (!FluidHelper.hasBlockState(fluid.getFluid()))
			return true;

		if (!fluidState.isEmpty() && FluidHelper.convertToStill(fluidState.getType()) != fluid.getFluid()) {
			FluidReactions.handlePipeSpillCollision(world, outputPos, fluid.getFluid(), fluidState);
			return false;
		}

		if (fluidState.isSource())
			return false;
		if (waterlog && fluid.getFluid() != Fluids.WATER)
			return false;
		if (simulate)
			return true;

		if (!AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get())
			return true;

		if (world.environmentAttributes()
			.getValue(EnvironmentAttributes.WATER_EVAPORATES, outputPos) && FluidHelper.isTag(fluid, FluidTags.WATER)) {
			int i = outputPos.getX();
			int j = outputPos.getY();
			int k = outputPos.getZ();
			world.playSound(null, i, j, k, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
				2.6F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8F);
			return true;
		}

		if (waterlog) {
			world.setBlock(outputPos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
			world.scheduleTick(outputPos, Fluids.WATER, 1);
			return true;
		}

		world.setBlock(outputPos, fluid.getFluid()
			.defaultFluidState()
			.createLegacyBlock(), Block.UPDATE_ALL);
		return true;
	}

	/**
	 * The open end exchanges fluid with the world, which cannot be rolled back, so the world half is
	 * simulated during the transaction and performed once it commits. The 1000mB buffer underneath is
	 * an ordinary resource handler.
	 */
	private class OpenEndFluidHandler extends FluidStacksResourceHandler {

		private final WorldJournal journal = new WorldJournal();
		private @Nullable FluidStack pendingProvide;
		private @Nullable FluidStack pendingEffect;
		private boolean pendingRemove;

		public OpenEndFluidHandler() {
			super(1, 1000);
		}

		private FluidStack contained() {
			return FluidUtil.getStack(this, 0);
		}

		@Override
		public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			// Never allow being filled when a source is attached
			if (world == null || !world.isLoaded(outputPos) || resource.isEmpty() || amount <= 0)
				return 0;

			FluidStack offered = resource.toStack(amount);
			if (!provideFluidToSpace(offered, true))
				return 0;

			FluidStack containedFluidStack = contained();
			boolean hasBlockState = FluidHelper.hasBlockState(resource.getFluid());

			journal.updateSnapshots(transaction);

			if (!containedFluidStack.isEmpty() && !resource.equals(FluidResource.of(containedFluidStack)))
				set(0, FluidResource.EMPTY, 0);
			if (wasPulling)
				wasPulling = false;

			OpenPipeEffectHandler effectHandler = OpenPipeEffectHandler.REGISTRY.get(resource.getFluid());
			// Fluids without a block form are consumed a droplet at a time, purely for their effect.
			int offeredAmount = effectHandler != null && !hasBlockState ? 1 : amount;

			int filled = super.insert(tank, resource, offeredAmount, transaction);

			if (effectHandler != null)
				pendingEffect = resource.toStack(offeredAmount);
			if (getAmountAsInt(0) == 1000 || !hasBlockState)
				pendingProvide = containedFluidStack.isEmpty() ? resource.toStack(offeredAmount)
					: containedFluidStack;

			return filled;
		}

		@Override
		public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
			if (world == null || !world.isLoaded(outputPos) || amount <= 0)
				return 0;
			if (amount > 1000)
				amount = 1000;

			if (!wasPulling)
				wasPulling = true;

			int drainedFromInternal = super.extract(tank, resource, amount, transaction);
			if (drainedFromInternal > 0)
				return drainedFromInternal;

			FluidStack drainedFromWorld = removeFluidFromSpace(true);
			if (drainedFromWorld.isEmpty() || !resource.equals(FluidResource.of(drainedFromWorld)))
				return 0;

			journal.updateSnapshots(transaction);
			pendingRemove = true;

			int remainder = drainedFromWorld.getAmount() - amount;
			if (remainder > 0) {
				if (!contained().isEmpty() && !resource.equals(FluidResource.of(contained())))
					set(0, FluidResource.EMPTY, 0);
				super.insert(tank, resource, remainder, transaction);
			}
			return Math.min(amount, drainedFromWorld.getAmount());
		}

		private class WorldJournal extends SnapshotJournal<Object[]> {
			@Override
			protected Object[] createSnapshot() {
				return new Object[] { pendingProvide, pendingEffect, pendingRemove };
			}

			@Override
			protected void revertToSnapshot(Object[] snapshot) {
				pendingProvide = (FluidStack) snapshot[0];
				pendingEffect = (FluidStack) snapshot[1];
				pendingRemove = (Boolean) snapshot[2];
			}

			@Override
			protected void onRootCommit(Object[] originalState) {
				if (pendingEffect != null) {
					OpenPipeEffectHandler effectHandler = OpenPipeEffectHandler.REGISTRY.get(pendingEffect.getFluid());
					if (effectHandler != null)
						effectHandler.apply(world, aoe, pendingEffect.copy());
				}
				if (pendingProvide != null && provideFluidToSpace(pendingProvide, false))
					set(0, FluidResource.EMPTY, 0);
				if (pendingRemove)
					removeFluidFromSpace(false);

				pendingProvide = null;
				pendingEffect = null;
				pendingRemove = false;
			}
		}

	}
}
