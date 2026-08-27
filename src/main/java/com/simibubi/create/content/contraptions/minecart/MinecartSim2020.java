package com.simibubi.create.content.contraptions.minecart;

import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * Useful methods for dealing with Minecarts
 *
 */
public class MinecartSim2020 {
	private static final Map<RailShape, Pair<Vec3i, Vec3i>> MATRIX =
		Util.make(Maps.newEnumMap(RailShape.class), (map) -> {
			Vec3i west = Direction.WEST.getUnitVec3i();
			Vec3i east = Direction.EAST.getUnitVec3i();
			Vec3i north = Direction.NORTH.getUnitVec3i();
			Vec3i south = Direction.SOUTH.getUnitVec3i();
			map.put(RailShape.NORTH_SOUTH, Pair.of(north, south));
			map.put(RailShape.EAST_WEST, Pair.of(west, east));
			map.put(RailShape.ASCENDING_EAST, Pair.of(west.below(), east));
			map.put(RailShape.ASCENDING_WEST, Pair.of(west, east.below()));
			map.put(RailShape.ASCENDING_NORTH, Pair.of(north, south.below()));
			map.put(RailShape.ASCENDING_SOUTH, Pair.of(north.below(), south));
			map.put(RailShape.SOUTH_EAST, Pair.of(south, east));
			map.put(RailShape.SOUTH_WEST, Pair.of(south, west));
			map.put(RailShape.NORTH_WEST, Pair.of(north, west));
			map.put(RailShape.NORTH_EAST, Pair.of(north, east));
		});

	public static Vec3 predictNextPositionOf(AbstractMinecart cart) {
		Vec3 position = cart.position();
		Vec3 motion = VecHelper.clamp(cart.getDeltaMovement(), 1f);
		return position.add(motion);
	}

	public static boolean canAddMotion(AbstractMinecart c) {
		if (c instanceof MinecartFurnace furnace)
			return furnace.push.equals(Vec3.ZERO);

		MinecartController controller = c.getData(AllAttachmentTypes.MINECART_CONTROLLER);
		if (controller.isPresent())
			return !controller.isStalled();
		return true;
	}

	public static void moveCartAlongTrack(AbstractMinecart cart, Vec3 forcedMovement, BlockPos cartPos,
		BlockState trackState) {

		if (forcedMovement.equals(Vec3.ZERO))
			return;
		if (!(cart.getBehavior() instanceof OldMinecartBehavior behavior))
			return;
		if (!(cart.level() instanceof ServerLevel serverLevel))
			return;

		Vec3 previousMotion = cart.getDeltaMovement();
		cart.fallDistance = 0.0F;

		double x = cart.getX();
		double y = cart.getY();
		double z = cart.getZ();

		double actualX = x;
		double actualY = y;
		double actualZ = z;

		Vec3 actualVec = behavior.getPos(actualX, actualY, actualZ);
		actualY = cartPos.getY() + 1;

		BaseRailBlock abstractrailblock = (BaseRailBlock) trackState.getBlock();
		RailShape railshape = abstractrailblock.getRailDirection(trackState, cart.level(), cartPos, cart);
		switch (railshape) {
		case ASCENDING_EAST:
			forcedMovement = forcedMovement.add(-slopeAdjustment(cart), 0.0D, 0.0D);
			actualY++;
			break;
		case ASCENDING_WEST:
			forcedMovement = forcedMovement.add(slopeAdjustment(cart), 0.0D, 0.0D);
			actualY++;
			break;
		case ASCENDING_NORTH:
			forcedMovement = forcedMovement.add(0.0D, 0.0D, slopeAdjustment(cart));
			actualY++;
			break;
		case ASCENDING_SOUTH:
			forcedMovement = forcedMovement.add(0.0D, 0.0D, -slopeAdjustment(cart));
			actualY++;
		default:
			break;
		}

		Pair<Vec3i, Vec3i> pair = MATRIX.get(railshape);
		Vec3i Vector3i = pair.getFirst();
		Vec3i Vector3i1 = pair.getSecond();
		double d4 = (double) (Vector3i1.getX() - Vector3i.getX());
		double d5 = (double) (Vector3i1.getZ() - Vector3i.getZ());
//		double d6 = Math.sqrt(d4 * d4 + d5 * d5);
		double d7 = forcedMovement.x * d4 + forcedMovement.z * d5;
		if (d7 < 0.0D) {
			d4 = -d4;
			d5 = -d5;
		}

		double d23 = (double) cartPos.getX() + 0.5D + (double) Vector3i.getX() * 0.5D;
		double d10 = (double) cartPos.getZ() + 0.5D + (double) Vector3i.getZ() * 0.5D;
		double d12 = (double) cartPos.getX() + 0.5D + (double) Vector3i1.getX() * 0.5D;
		double d13 = (double) cartPos.getZ() + 0.5D + (double) Vector3i1.getZ() * 0.5D;
		d4 = d12 - d23;
		d5 = d13 - d10;
		double d14;
		if (d4 == 0.0D) {
			d14 = actualZ - (double) cartPos.getZ();
		} else if (d5 == 0.0D) {
			d14 = actualX - (double) cartPos.getX();
		} else {
			double d15 = actualX - d23;
			double d16 = actualZ - d10;
			d14 = (d15 * d4 + d16 * d5) * 2.0D;
		}

		actualX = d23 + d4 * d14;
		actualZ = d10 + d5 * d14;

		cart.setPos(actualX, actualY, actualZ);
		cart.setDeltaMovement(forcedMovement);
		behavior.moveAlongTrack(serverLevel);

		x = cart.getX();
		y = cart.getY();
		z = cart.getZ();

		if (Vector3i.getY() != 0 && Mth.floor(x) - cartPos.getX() == Vector3i.getX()
			&& Mth.floor(z) - cartPos.getZ() == Vector3i.getZ()) {
			cart.setPos(x, y + (double) Vector3i.getY(), z);
		} else if (Vector3i1.getY() != 0 && Mth.floor(x) - cartPos.getX() == Vector3i1.getX()
			&& Mth.floor(z) - cartPos.getZ() == Vector3i1.getZ()) {
			cart.setPos(x, y + (double) Vector3i1.getY(), z);
		}

		x = cart.getX();
		y = cart.getY();
		z = cart.getZ();

		Vec3 Vector3d3 = behavior.getPos(x, y, z);
		if (Vector3d3 != null && actualVec != null) {
			double d17 = (actualVec.y - Vector3d3.y) * 0.05D;
			Vec3 Vector3d4 = cart.getDeltaMovement();
			double d18 = Math.sqrt(Vector3d4.horizontalDistanceSqr());
			if (d18 > 0.0D) {
				cart.setDeltaMovement(Vector3d4.multiply((d18 + d17) / d18, 1.0D, (d18 + d17) / d18));
			}

			cart.setPos(x, Vector3d3.y, z);
		}

		x = cart.getX();
		y = cart.getY();
		z = cart.getZ();

		int j = Mth.floor(x);
		int i = Mth.floor(z);
		if (j != cartPos.getX() || i != cartPos.getZ()) {
			Vec3 Vector3d5 = cart.getDeltaMovement();
			double d26 = Math.sqrt(Vector3d5.horizontalDistanceSqr());
			cart.setDeltaMovement(d26 * (double) (j - cartPos.getX()), Vector3d5.y, d26 * (double) (i - cartPos.getZ()));
		}

		cart.setDeltaMovement(previousMotion);
	}

	/**
	 * How hard a slope pulls a cart downhill.
	 * <p>
	 * NeoForge's per-cart hook for this is gone in 26.2; the value the vanilla behaviour uses is a
	 * constant, softened in water.
	 */
	private static double slopeAdjustment(AbstractMinecart cart) {
		return cart.isInWater() ? 0.0078125 * 0.2 : 0.0078125;
	}

	public static Vec3 getRailVec(RailShape shape) {
		switch (shape) {
		case ASCENDING_NORTH:
		case ASCENDING_SOUTH:
		case NORTH_SOUTH:
			return new Vec3(0, 0, 1);
		case ASCENDING_EAST:
		case ASCENDING_WEST:
		case EAST_WEST:
			return new Vec3(1, 0, 0);
		case NORTH_EAST:
		case SOUTH_WEST:
			return new Vec3(1, 0, 1).normalize();
		case NORTH_WEST:
		case SOUTH_EAST:
			return new Vec3(1, 0, -1).normalize();
		default:
			return new Vec3(0, 1, 0);
		}
	}


	/**
	 * How fast a cart may travel.
	 * <p>
	 * NeoForge's per-rail speed hook is gone in 26.2; a cart's own limit is what remains, and it is only
	 * defined on the server, so the vanilla ground speed stands in elsewhere.
	 */
	public static float maxSpeed(AbstractMinecart cart) {
		return cart.level() instanceof ServerLevel serverLevel ? (float) cart.getMaxSpeed(serverLevel) : 0.4f;
	}

}
