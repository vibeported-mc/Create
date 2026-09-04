package com.simibubi.create.content.trains.graph;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import net.minecraft.world.entity.player.Player;

public abstract class TrackGraphPacket implements CustomPacketPayload {

	public UUID graphId;
	public int netId;
	public boolean packetDeletesGraph;

	public void handle(Player player) {
		this.handle(CreateClient.RAILWAYS, CreateClient.RAILWAYS.getOrCreateGraph(graphId, netId));
	}

	protected abstract void handle(GlobalRailwayManager manager, TrackGraph graph);

}
