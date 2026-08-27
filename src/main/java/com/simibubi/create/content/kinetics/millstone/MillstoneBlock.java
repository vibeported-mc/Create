package com.simibubi.create.content.kinetics.millstone;

import com.simibubi.create.foundation.item.ItemHandlerHelpers;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import com.simibubi.create.foundation.item.ModifiableItemHandler;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.capabilities.Capabilities;
public class MillstoneBlock extends KineticBlock implements IBE<MillstoneBlockEntity>, ICogWheel {

	public MillstoneBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.MILLSTONE;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.isEmpty())
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		withBlockEntityDo(level, pos, millstone -> {
			boolean emptyOutput = true;
			ModifiableItemHandler inv = millstone.outputInv;
			for (int slot = 0; slot < inv.size(); slot++) {
				ItemStack stackInSlot = ItemHandlerHelpers.getStackInSlot(inv, slot);
				if (!stackInSlot.isEmpty())
					emptyOutput = false;
				player.getInventory()
					.placeItemBackInInventory(stackInSlot);
				ItemHandlerHelpers.setStackInSlot(inv, slot, ItemStack.EMPTY);
			}

			if (emptyOutput) {
				inv = millstone.inputInv;
				for (int slot = 0; slot < inv.size(); slot++) {
					player.getInventory()
						.placeItemBackInInventory(ItemHandlerHelpers.getStackInSlot(inv, slot));
					ItemHandlerHelpers.setStackInSlot(inv, slot, ItemStack.EMPTY);
				}
			}

			millstone.setChanged();
			millstone.sendData();
		});

		return InteractionResult.SUCCESS;
	}

	@Override
	public void fallOn(Level worldIn, BlockState fallenOn, BlockPos fallenOnPos, Entity entityIn,
		double fallDistance) {
		super.fallOn(worldIn, fallenOn, fallenOnPos, entityIn, fallDistance);

		if (entityIn.level().isClientSide())
			return;
		if (!(entityIn instanceof ItemEntity itemEntity))
			return;
		if (!entityIn.isAlive())
			return;

		MillstoneBlockEntity millstone = null;
		for (BlockPos pos : Iterate.hereAndBelow(entityIn.blockPosition()))
			if (millstone == null)
				millstone = getBlockEntity(worldIn, pos);

		if (millstone == null)
			return;

		ResourceHandler<ItemResource> capability = millstone.getLevel().getCapability(Capabilities.Item.BLOCK, millstone.getBlockPos(), null);
		if (capability == null)
			return;

		ItemStack remainder = ItemHandlerHelpers.insertItem(capability, 0, itemEntity.getItem(), false);
		if (remainder.isEmpty())
			itemEntity.discard();
		if (remainder.getCount() < itemEntity.getItem()
			.getCount())
			itemEntity.setItem(remainder);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	public Class<MillstoneBlockEntity> getBlockEntityClass() {
		return MillstoneBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MillstoneBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MILLSTONE.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

}
