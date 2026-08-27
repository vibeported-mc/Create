package com.simibubi.create.content.contraptions.actors.seat;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.NullMarked;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Optional;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTags.AllEntityTags;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.common.util.FakePlayer;

@NullMarked
public class SeatBlock extends Block implements ProperWaterloggedBlock {

	protected final DyeColor color;

	public SeatBlock(Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(WATERLOGGED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return withWater(super.getStateForPlacement(pContext), pContext);
	}

	@Override
	public BlockState updateShape(BlockState pState, LevelReader pLevel, ScheduledTickAccess ticks,
		BlockPos pCurrentPos, Direction pDirection, BlockPos pNeighborPos, BlockState pNeighborState, RandomSource random) {
		updateWater(pLevel, ticks, pState, pCurrentPos);
		return pState;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	/**
	 * A seat that will not take this passenger springs it back off instead. The entity applies the
	 * bounce itself in 26.2, so only the condition stays here.
	 */
	@Override
	public float getBounceRestitution(Level level, BlockPos pos, BlockState blockState, Entity entity) {
		return seats(entity, pos) ? 0 : 0.66f;
	}

	@Override
	public void fallOn(Level level, BlockState fallenOn, BlockPos fallenOnPos, Entity entity, double fallDistance) {
		// Seats halve fall damage, and catch anything that can be seated.
		super.fallOn(level, fallenOn, fallenOnPos, entity, fallDistance * 0.5);
		BlockPos pos = entity.blockPosition();
		if (!seats(entity, pos))
			return;
		if (level.getBlockState(pos)
			.getBlock() != this)
			return;
		sitDown(level, pos, entity);
	}

	private boolean seats(Entity entity, BlockPos pos) {
		return !(entity instanceof Player) && entity instanceof LivingEntity && canBePickedUp(entity)
			&& !isSeatOccupied(entity.level(), pos);
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, @Nullable Mob entity) {
		return PathType.RAIL;
	}

	@Override
	public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_,
							   CollisionContext p_220053_4_) {
		return AllShapes.SEAT;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_,
										CollisionContext ctx) {
		if (ctx instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player player)
			return AllShapes.SEAT_COLLISION_PLAYERS;
		return AllShapes.SEAT_COLLISION;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (player.isShiftKeyDown() || player instanceof FakePlayer)
			return InteractionResult.TRY_WITH_EMPTY_HAND;

		DyeColor color = DyeColor.getColor(stack);
		if (color != null && color != this.color) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			BlockState newState = BlockHelper.copyProperties(state, AllBlocks.SEATS.get(color)
				.getDefaultState());
			level.setBlockAndUpdate(pos, newState);
			return InteractionResult.SUCCESS;
		}

		List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
		if (!seats.isEmpty()) {
			SeatEntity seatEntity = seats.get(0);
			List<Entity> passengers = seatEntity.getPassengers();
			if (!passengers.isEmpty() && passengers.get(0) instanceof Player)
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			if (!level.isClientSide()) {
				seatEntity.ejectPassengers();
				player.startRiding(seatEntity);
			}
			return InteractionResult.SUCCESS;
		}

		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		sitDown(level, pos, getLeashed(level, player).or(player));
		return InteractionResult.SUCCESS;
	}

	public static boolean isSeatOccupied(Level world, BlockPos pos) {
		return !world.getEntitiesOfClass(SeatEntity.class, new AABB(pos))
			.isEmpty();
	}

	public static Optional<Entity> getLeashed(Level level, Player player) {
		List<Entity> entities = player.level().getEntities((Entity) null, player.getBoundingBox()
			.inflate(10), e -> true);
		for (Entity e : entities)
			if (e instanceof Mob mob && mob.getLeashHolder() == player && SeatBlock.canBePickedUp(e))
				return Optional.of(mob);
		return Optional.absent();
	}

	public static boolean canBePickedUp(Entity passenger) {
		if (passenger instanceof Shulker)
			return false;
		if (passenger instanceof Player)
			return false;
		if (AllEntityTags.IGNORE_SEAT.matches(passenger))
			return false;
		if (!AllConfigs.server().logistics.seatHostileMobs.get() && !passenger.getType()
			.getCategory()
			.isFriendly())
			return false;
		return passenger instanceof LivingEntity;
	}

	public static void sitDown(Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide())
			return;
		SeatEntity seat = new SeatEntity(level);
		seat.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		level.addFreshEntity(seat);
		entity.startRiding(seat, true);
		if (entity instanceof TamableAnimal ta)
			ta.setInSittingPose(true);
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

}
