package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.data.Pair;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shows why a large water wheel would not fit, to the player who tried to place it.
 * <p>
 * A class of its own rather than a method on {@link LargeWaterWheelBlockItem}, because it names
 * {@link LocalPlayer}. The JVM verifies a class as a whole when it links it, so a common class
 * holding this code cannot be loaded on a dedicated server at all -- and the item is loaded there,
 * during registration. Until 26.2 {@code @OnlyIn} stripped such members and the question did not
 * arise; NeoForge no longer strips them, so the code has to live where only a client will load it.
 */
public class LargeWaterWheelPlacementHint {

	public static void show(BlockPlaceContext context, Axis axis) {
		if (!(context.getPlayer() instanceof LocalPlayer localPlayer))
			return;

		BlockPos pos = context.getClickedPos();
		Vec3 contract = Vec3.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, axis)
			.getUnitVec3i());

		Outliner.getInstance()
			.showAABB(Pair.of("waterwheel", pos), new AABB(pos).inflate(1)
				.deflate(contract.x, contract.y, contract.z))
			.colored(0xFF_ff5d6c);
		CreateLang.translate("large_water_wheel.not_enough_space")
			.color(0xFF_ff5d6c)
			.sendStatus(localPlayer);
	}

	private LargeWaterWheelPlacementHint() {}
}
