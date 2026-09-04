package com.simibubi.create.foundation.utility;

import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simibubi.create.AllPackets;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.codec.StreamCodec;

public class ServerSpeedProvider {
	private static final LerpedFloat modifier = LerpedFloat.linear();

	private static int clientTimer = 0;
	private static int serverTimer = 0;
	private static boolean initialized = false;

	public static void serverTick() {
		serverTimer++;
		if (serverTimer > getSyncInterval()) {
			NetworkHelper.INSTANCE.sendToAllClients(Packet.INSTANCE);
			serverTimer = 0;
		}
	}

	public static void clientTick() {
		if (Minecraft.getInstance()
			.hasSingleplayerServer()
			&& Minecraft.getInstance()
				.isPaused())
			return;
		modifier.tickChaser();
		clientTimer++;
	}

	public static Integer getSyncInterval() {
		return AllConfigs.server().tickrateSyncTimer.get();
	}

	public static float get() {
		return modifier.getValue();
	}

	public enum Packet implements CustomPacketPayload {
		INSTANCE;

		public static final StreamCodec<ByteBuf, Packet> STREAM_CODEC = StreamCodec.unit(INSTANCE);

		public void handle(Player player) {
			if (!initialized) {
				initialized = true;
				clientTimer = 0;
				return;
			}
			float target = ((float) getSyncInterval()) / Math.max(clientTimer, 1);
			modifier.chase(Math.min(target, 1), .25, Chaser.EXP);
			// Set this to -1 because packets are processed before ticks.
			// ServerSpeedProvider#clientTick will increment it to 0 at the end of this tick.
			// Setting it to 0 causes consistent desync, as the client ends up counting too many ticks.
			clientTimer = -1;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return AllPackets.SERVER_SPEED.getType();
		}
	}

}
