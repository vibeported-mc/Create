package com.simibubi.create.content.contraptions.sync;

import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import com.simibubi.create.AllPackets;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record ClientMotionPacket(Vec3 motion, boolean onGround, float limbSwing) implements SelfHandlingPayload {
	public static final StreamCodec<ByteBuf, ClientMotionPacket> STREAM_CODEC = StreamCodec.composite(
			CatnipStreamCodecs.VEC3, ClientMotionPacket::motion,
			ByteBufCodecs.BOOL, ClientMotionPacket::onGround,
			ByteBufCodecs.FLOAT, ClientMotionPacket::limbSwing,
	        ClientMotionPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.CLIENT_MOTION.getType();
	}

	@Override
	public void handle(ServerPlayer sender) {
		if (sender == null)
			return;
		sender.setDeltaMovement(motion);
		sender.setOnGround(onGround);
		if (onGround) {
			sender.causeFallDamage(sender.fallDistance, 1, sender.damageSources().fall());
			sender.fallDistance = 0;
			sender.connection.aboveGroundTickCount = 0;
			sender.connection.aboveGroundVehicleTickCount = 0;
		}
		NetworkHelper.INSTANCE.sendToClientsTrackingEntity(sender,
				new LimbSwingUpdatePacket(sender.getId(), sender.position(), limbSwing));
	}
}
