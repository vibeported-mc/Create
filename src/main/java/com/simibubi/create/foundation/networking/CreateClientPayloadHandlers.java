package com.simibubi.create.foundation.networking;

import com.simibubi.create.AllPackets;
import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.trainmap.TrainMapSyncPacket;
import com.simibubi.create.compat.trainmap.TrainMapSyncRequestPacket;
import com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket;
import com.simibubi.create.content.contraptions.ContraptionDisassemblyPacket;
import com.simibubi.create.content.contraptions.ContraptionRelocationPacket;
import com.simibubi.create.content.contraptions.ContraptionStallPacket;
import com.simibubi.create.content.contraptions.MountedStorageSyncPacket;
import com.simibubi.create.content.contraptions.TrainCollisionPacket;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionDisableActorPacket;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsStopControllingPacket;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactEditPacket;
import com.simibubi.create.content.contraptions.elevator.ElevatorFloorListPacket;
import com.simibubi.create.content.contraptions.elevator.ElevatorTargetFloorPacket;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionUpdatePacket;
import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.simibubi.create.content.contraptions.glue.SuperGlueRemovalPacket;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionPacket;
import com.simibubi.create.content.contraptions.minecart.CouplingCreationPacket;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartControllerUpdatePacket;
import com.simibubi.create.content.contraptions.sync.ClientMotionPacket;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import com.simibubi.create.content.contraptions.sync.ContraptionSeatMappingPacket;
import com.simibubi.create.content.contraptions.sync.LimbSwingUpdatePacket;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectPacket;
import com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket;
import com.simibubi.create.content.equipment.clipboard.ClipboardEditPacket;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripInteractionPacket;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonPacket;
import com.simibubi.create.content.equipment.symmetryWand.ConfigureSymmetryWandPacket;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.simibubi.create.content.equipment.tool.KnockbackPacket;
import com.simibubi.create.content.equipment.toolbox.ToolboxDisposeAllPacket;
import com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket;
import com.simibubi.create.content.equipment.zapper.ZapperBeamPacket;
import com.simibubi.create.content.equipment.zapper.terrainzapper.ConfigureWorldshaperPacket;
import com.simibubi.create.content.fluids.transfer.FluidSplashPacket;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionPacket;
import com.simibubi.create.content.kinetics.chainConveyor.ChainPackageInteractionPacket;
import com.simibubi.create.content.kinetics.chainConveyor.ClientboundChainConveyorRidingPacket;
import com.simibubi.create.content.kinetics.chainConveyor.ServerboundChainConveyorRidingPacket;
import com.simibubi.create.content.kinetics.gauge.GaugeObservedPacket;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.simibubi.create.content.kinetics.transmission.sequencer.ConfigureSequencedGearshiftPacket;
import com.simibubi.create.content.logistics.box.PackageDestroyPacket;
import com.simibubi.create.content.logistics.depot.EjectorAwardPacket;
import com.simibubi.create.content.logistics.depot.EjectorElytraPacket;
import com.simibubi.create.content.logistics.depot.EjectorPlacementPacket;
import com.simibubi.create.content.logistics.depot.EjectorTriggerPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket;
import com.simibubi.create.content.logistics.funnel.FunnelFlapPacket;
import com.simibubi.create.content.logistics.packagePort.PackagePortConfigurationPacket;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterConfigurationPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockRequestPacket;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderRequestPacket;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryEditPacket;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryHidingPacket;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryRefundPacket;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperLockPacket;
import com.simibubi.create.content.logistics.tableCloth.ShopUpdatePacket;
import com.simibubi.create.content.logistics.tunnel.TunnelFlapPacket;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkConfigurationPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerBindPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerStopLecternPacket;
import com.simibubi.create.content.redstone.thresholdSwitch.ConfigureThresholdSwitchPacket;
import com.simibubi.create.content.schematics.cannon.ConfigureSchematicannonPacket;
import com.simibubi.create.content.schematics.packet.InstantSchematicPacket;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import com.simibubi.create.content.schematics.packet.SchematicUploadPacket;
import com.simibubi.create.content.trains.HonkPacket;
import com.simibubi.create.content.trains.TrainHUDUpdatePacket;
import com.simibubi.create.content.trains.entity.AddTrainPacket;
import com.simibubi.create.content.trains.entity.RemoveTrainPacket;
import com.simibubi.create.content.trains.entity.TrainPromptPacket;
import com.simibubi.create.content.trains.entity.TrainRelocationPacket;
import com.simibubi.create.content.trains.graph.TrackGraphRequestPacket;
import com.simibubi.create.content.trains.graph.TrackGraphRollCallPacket;
import com.simibubi.create.content.trains.graph.TrackGraphSyncPacket;
import com.simibubi.create.content.trains.schedule.ScheduleEditPacket;
import com.simibubi.create.content.trains.signal.SignalEdgeGroupPacket;
import com.simibubi.create.content.trains.station.StationEditPacket;
import com.simibubi.create.content.trains.station.TrainEditPacket.TrainEditReturnPacket;
import com.simibubi.create.content.trains.station.TrainEditPacket;
import com.simibubi.create.content.trains.track.CurvedTrackDestroyPacket;
import com.simibubi.create.content.trains.track.CurvedTrackSelectionPacket;
import com.simibubi.create.content.trains.track.PlaceExtendedCurvePacket;
import com.simibubi.create.foundation.blockEntity.RemoveBlockEntityPacket;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;
import com.simibubi.create.foundation.gui.menu.ClearMenuPacket;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import com.simibubi.create.foundation.networking.LeftClickPacket;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.infrastructure.command.HighlightPacket;
import com.simibubi.create.infrastructure.debugInfo.ServerDebugInfoPacket;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Clientbound payload handlers.
 * <p>
 * Catnip's 26.x network API deliberately has no clientbound equivalent of
 * {@code SelfHandlingPayload}: a payload class is loaded on both sides, so a handler that touches
 * client-only types cannot live on it. The handlers stay as ordinary methods on each payload and are
 * wired up here, from a class that only ever loads on the client.
 */
@OnlyIn(Dist.CLIENT)
public class CreateClientPayloadHandlers {

	public static void register() {
		register(AllPackets.SYMMETRY_EFFECT, SymmetryEffectPacket::handle);
		register(AllPackets.SERVER_SPEED, ServerSpeedProvider.Packet::handle);
		register(AllPackets.BEAM_EFFECT, ZapperBeamPacket::handle);
		register(AllPackets.CONTRAPTION_STALL, ContraptionStallPacket::handle);
		register(AllPackets.CONTRAPTION_DISASSEMBLE, ContraptionDisassemblyPacket::handle);
		register(AllPackets.CONTRAPTION_BLOCK_CHANGED, ContraptionBlockChangedPacket::handle);
		register(AllPackets.GLUE_EFFECT, GlueEffectPacket::handle);
		register(AllPackets.CONTRAPTION_SEAT_MAPPING, ContraptionSeatMappingPacket::handle);
		register(AllPackets.LIMBSWING_UPDATE, LimbSwingUpdatePacket::handle);
		register(AllPackets.MINECART_CONTROLLER, MinecartControllerUpdatePacket::handle);
		register(AllPackets.FLUID_SPLASH, FluidSplashPacket::handle);
		register(AllPackets.MOUNTED_STORAGE_SYNC, MountedStorageSyncPacket::handle);
		register(AllPackets.GANTRY_UPDATE, GantryContraptionUpdatePacket::handle);
		register(AllPackets.BLOCK_HIGHLIGHT, HighlightPacket::handle);
		register(AllPackets.TUNNEL_FLAP, TunnelFlapPacket::handle);
		register(AllPackets.FUNNEL_FLAP, FunnelFlapPacket::handle);
		register(AllPackets.POTATO_CANNON, PotatoCannonPacket::handle);
		register(AllPackets.SOUL_PULSE, SoulPulseEffectPacket::handle);
		register(AllPackets.PERSISTENT_DATA, ISyncPersistentData.PersistentDataPacket::handle);
		register(AllPackets.SYNC_RAIL_GRAPH, TrackGraphSyncPacket::handle);
		register(AllPackets.SYNC_EDGE_GROUP, SignalEdgeGroupPacket::handle);
		register(AllPackets.ADD_TRAIN, AddTrainPacket::handle);
		register(AllPackets.REMOVE_TRAIN, RemoveTrainPacket::handle);
		register(AllPackets.REMOVE_TE, RemoveBlockEntityPacket::handle);
		register(AllPackets.S_CONFIGURE_TRAIN, TrainEditReturnPacket::handle);
		register(AllPackets.CONTROLS_ABORT, ControlsStopControllingPacket::handle);
		register(AllPackets.S_TRAIN_HUD, TrainHUDUpdatePacket.Clientbound::handle);
		register(AllPackets.S_TRAIN_HONK, HonkPacket.Clientbound::handle);
		register(AllPackets.S_TRAIN_PROMPT, TrainPromptPacket::handle);
		register(AllPackets.CONTRAPTION_RELOCATION, ContraptionRelocationPacket::handle);
		register(AllPackets.TRACK_GRAPH_ROLL_CALL, TrackGraphRollCallPacket::handle);
		register(AllPackets.S_PLACE_ARM, ArmPlacementPacket.ClientBoundRequest::handle);
		register(AllPackets.S_PLACE_EJECTOR, EjectorPlacementPacket.ClientBoundRequest::handle);
		register(AllPackets.S_PLACE_PACKAGE_PORT, PackagePortPlacementPacket.ClientBoundRequest::handle);
		register(AllPackets.UPDATE_ELEVATOR_FLOORS, ElevatorFloorListPacket::handle);
		register(AllPackets.CONTRAPTION_ACTOR_TOGGLE, ContraptionDisableActorPacket::handle);
		register(AllPackets.CONTRAPTION_COLLIDER_LOCK, ContraptionColliderLockPacket::handle);
		register(AllPackets.ATTACHED_COMPUTER, AttachedComputerPacket::handle);
		register(AllPackets.SERVER_DEBUG_INFO, ServerDebugInfoPacket::handle);
		register(AllPackets.PACKAGE_DESTROYED, PackageDestroyPacket::handle);
		register(AllPackets.LOGISTICS_STOCK_RESPONSE, LogisticalStockResponsePacket::handle);
		register(AllPackets.FACTORY_PANEL_EFFECT, FactoryPanelEffectPacket::handle);
		register(AllPackets.PACKAGER_LINK_EFFECT, WiFiEffectPacket::handle);
		register(AllPackets.REDSTONE_REQUESTER_EFFECT, RedstoneRequesterEffectPacket::handle);
		register(AllPackets.KNOCKBACK, KnockbackPacket::handle);
		register(AllPackets.TRAIN_MAP_SYNC, TrainMapSyncPacket::handle);
		register(AllPackets.CLIENTBOUND_CHAIN_CONVEYOR, ClientboundChainConveyorRidingPacket::handle);
		register(AllPackets.SHOP_UPDATE, ShopUpdatePacket::handle);
	}

	private static <T extends CustomPacketPayload> void register(AllPackets packet, Handler<T> handler) {
		ClientNetworkHelper.INSTANCE.registerPayloadHandler(packet.getType(), handler::handle);
	}

	@FunctionalInterface
	private interface Handler<T extends CustomPacketPayload> {
		void handle(T payload, LocalPlayer player);
	}

}
