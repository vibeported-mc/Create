package com.simibubi.create.content.kinetics.crank;


import org.jspecify.annotations.NullMarked;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@NullMarked
@EventBusSubscriber
public class ValveHandleBlock extends HandCrankBlock {

	public final DyeColor color;

	public static ValveHandleBlock copper(Properties properties) {
		return new ValveHandleBlock(properties, null);
	}

	/**
	 * Dyeing a valve handle swaps it for the handle of that colour, a different block, which should not
	 * cost it its block entity and its place in the kinetic network.
	 * <p>
	 * 1.21.1 kept the block entity by declining to remove it in {@code onRemove}. 26.2 has no
	 * {@code onRemove}: the chunk removes a block entity itself whenever the block changes, unless the
	 * <em>new</em> block asks to keep it here.
	 */
	@Override
	protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
		return AllBlockEntityTypes.VALVE_HANDLE.get().isValid(oldState);
	}

	public static ValveHandleBlock dyed(Properties properties, DyeColor color) {
		return new ValveHandleBlock(properties, color);
	}

	private ValveHandleBlock(Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.VALVE_HANDLE.get(pState.getValue(FACING));
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onBlockActivated(PlayerInteractEvent.RightClickBlock event) {
		BlockPos pos = event.getPos();
		Level level = event.getLevel();
		Player player = event.getEntity();
		BlockState blockState = level.getBlockState(pos);

		if (!(blockState.getBlock() instanceof ValveHandleBlock vhb))
			return;
		if (!player.mayBuild())
			return;
		if (AllItems.WRENCH.isIn(player.getItemInHand(event.getHand())) && player.isShiftKeyDown())
			return;

		if (vhb.clicked(level, pos, blockState, player, event.getHand())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}

	public boolean clicked(Level level, BlockPos pos, BlockState blockState, Player player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		DyeColor color = DyeColor.getColor(heldItem);

		if (color != null && color != this.color) {
			if (!level.isClientSide())
				level.setBlockAndUpdate(pos,
					BlockHelper.copyProperties(blockState, AllBlocks.DYED_VALVE_HANDLES.get(color)
						.getDefaultState()));
			return true;
		}

		onBlockEntityUse(level, pos,
			hcbe -> (hcbe instanceof ValveHandleBlockEntity vhbe) && vhbe.activate(player.isShiftKeyDown())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS);
		return true;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.VALVE_HANDLE.get();
	}

	@Override
	public int getRotationSpeed() {
		return 32;
	}
}
