package com.simibubi.create;

import java.util.Locale;

import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.trainmap.TrainMapSyncPacket;
import com.simibubi.create.compat.trainmap.TrainMapSyncRequestPacket;
import com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket;
import com.simibubi.create.content.contraptions.ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest;
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
import com.simibubi.create.content.trains.station.TrainEditPacket;
import com.simibubi.create.content.trains.station.TrainEditPacket.TrainEditReturnPacket;
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

import net.createmod.catnip.api.network.SelfHandlingPayload;
import net.createmod.catnip.api.network.registry.CatnipPayloadRegistrar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum AllPackets {
	// Client to Server
	CONFIGURE_SCHEMATICANNON(Direction.TO_SERVER, ConfigureSchematicannonPacket.STREAM_CODEC),
	CONFIGURE_STOCKSWITCH(Direction.TO_SERVER, ConfigureThresholdSwitchPacket.STREAM_CODEC),
	CONFIGURE_SEQUENCER(Direction.TO_SERVER, ConfigureSequencedGearshiftPacket.STREAM_CODEC),
	PLACE_SCHEMATIC(Direction.TO_SERVER, SchematicPlacePacket.STREAM_CODEC),
	UPLOAD_SCHEMATIC(Direction.TO_SERVER, SchematicUploadPacket.STREAM_CODEC),
	CLEAR_CONTAINER(Direction.TO_SERVER, ClearMenuPacket.STREAM_CODEC),
	CONFIGURE_FILTER(Direction.TO_SERVER, FilterScreenPacket.STREAM_CODEC),
	EXTENDO_INTERACT(Direction.TO_SERVER, ExtendoGripInteractionPacket.STREAM_CODEC),
	CONTRAPTION_INTERACT(Direction.TO_SERVER, ContraptionInteractionPacket.STREAM_CODEC),
	CLIENT_MOTION(Direction.TO_SERVER, ClientMotionPacket.STREAM_CODEC),
	PLACE_ARM(Direction.TO_SERVER, ArmPlacementPacket.STREAM_CODEC),
	PLACE_PACKAGE_PORT(Direction.TO_SERVER, PackagePortPlacementPacket.STREAM_CODEC),
	MINECART_COUPLING_CREATION(Direction.TO_SERVER, CouplingCreationPacket.STREAM_CODEC),
	INSTANT_SCHEMATIC(Direction.TO_SERVER, InstantSchematicPacket.STREAM_CODEC),
	SYNC_SCHEMATIC(Direction.TO_SERVER, SchematicSyncPacket.STREAM_CODEC),
	LEFT_CLICK(Direction.TO_SERVER, LeftClickPacket.STREAM_CODEC),
	PLACE_EJECTOR(Direction.TO_SERVER, EjectorPlacementPacket.STREAM_CODEC),
	TRIGGER_EJECTOR(Direction.TO_SERVER, EjectorTriggerPacket.STREAM_CODEC),
	EJECTOR_ELYTRA(Direction.TO_SERVER, EjectorElytraPacket.STREAM_CODEC),
	LINKED_CONTROLLER_INPUT(Direction.TO_SERVER, LinkedControllerInputPacket.STREAM_CODEC),
	LINKED_CONTROLLER_BIND(Direction.TO_SERVER, LinkedControllerBindPacket.STREAM_CODEC),
	LINKED_CONTROLLER_USE_LECTERN(Direction.TO_SERVER, LinkedControllerStopLecternPacket.STREAM_CODEC),
	SUBMIT_GHOST_ITEM(Direction.TO_SERVER, GhostItemSubmitPacket.STREAM_CODEC),
	BLUEPRINT_COMPLETE_RECIPE(Direction.TO_SERVER, BlueprintAssignCompleteRecipePacket.STREAM_CODEC),
	CONFIGURE_SYMMETRY_WAND(Direction.TO_SERVER, ConfigureSymmetryWandPacket.STREAM_CODEC),
	CONFIGURE_WORLDSHAPER(Direction.TO_SERVER, ConfigureWorldshaperPacket.STREAM_CODEC),
	TOOLBOX_EQUIP(Direction.TO_SERVER, ToolboxEquipPacket.STREAM_CODEC),
	TOOLBOX_DISPOSE_ALL(Direction.TO_SERVER, ToolboxDisposeAllPacket.STREAM_CODEC),
	CONFIGURE_SCHEDULE(Direction.TO_SERVER, ScheduleEditPacket.STREAM_CODEC),
	CONFIGURE_STATION(Direction.TO_SERVER, StationEditPacket.STREAM_CODEC),
	C_CONFIGURE_TRAIN(Direction.TO_SERVER, TrainEditPacket.Serverbound.STREAM_CODEC),
	RELOCATE_TRAIN(Direction.TO_SERVER, TrainRelocationPacket.STREAM_CODEC),
	CONTROLS_INPUT(Direction.TO_SERVER, ControlsInputPacket.STREAM_CODEC),
	CONFIGURE_DATA_GATHERER(Direction.TO_SERVER, DisplayLinkConfigurationPacket.STREAM_CODEC),
	DESTROY_CURVED_TRACK(Direction.TO_SERVER, CurvedTrackDestroyPacket.STREAM_CODEC),
	SELECT_CURVED_TRACK(Direction.TO_SERVER, CurvedTrackSelectionPacket.STREAM_CODEC),
	PLACE_CURVED_TRACK(Direction.TO_SERVER, PlaceExtendedCurvePacket.STREAM_CODEC),
	GLUE_IN_AREA(Direction.TO_SERVER, SuperGlueSelectionPacket.STREAM_CODEC),
	GLUE_REMOVED(Direction.TO_SERVER, SuperGlueRemovalPacket.STREAM_CODEC),
	TRAIN_COLLISION(Direction.TO_SERVER, TrainCollisionPacket.STREAM_CODEC),
	C_TRAIN_HUD(Direction.TO_SERVER, TrainHUDUpdatePacket.Serverbound.STREAM_CODEC),
	C_TRAIN_HONK(Direction.TO_SERVER, HonkPacket.Serverbound.STREAM_CODEC),
	OBSERVER_STRESSOMETER(Direction.TO_SERVER, GaugeObservedPacket.STREAM_CODEC),
	EJECTOR_AWARD(Direction.TO_SERVER, EjectorAwardPacket.STREAM_CODEC),
	TRACK_GRAPH_REQUEST(Direction.TO_SERVER, TrackGraphRequestPacket.STREAM_CODEC),
	CONFIGURE_ELEVATOR_CONTACT(Direction.TO_SERVER, ElevatorContactEditPacket.STREAM_CODEC),
	REQUEST_FLOOR_LIST(Direction.TO_SERVER, ElevatorFloorListPacket.RequestFloorList.STREAM_CODEC),
	ELEVATOR_SET_FLOOR(Direction.TO_SERVER, ElevatorTargetFloorPacket.STREAM_CODEC),
	VALUE_SETTINGS(Direction.TO_SERVER, ValueSettingsPacket.STREAM_CODEC),
	CLIPBOARD_EDIT(Direction.TO_SERVER, ClipboardEditPacket.STREAM_CODEC),
	CONTRAPTION_COLLIDER_LOCK_REQUEST(Direction.TO_SERVER, ContraptionColliderLockPacketRequest.STREAM_CODEC),
	RADIAL_WRENCH_MENU_SUBMIT(Direction.TO_SERVER, RadialWrenchMenuSubmitPacket.STREAM_CODEC),
	LOGISTICS_STOCK_REQUEST(Direction.TO_SERVER, LogisticalStockRequestPacket.STREAM_CODEC),
	LOGISTICS_PACKAGE_REQUEST(Direction.TO_SERVER, PackageOrderRequestPacket.STREAM_CODEC),
	CHAIN_CONVEYOR_CONNECT(Direction.TO_SERVER, ChainConveyorConnectionPacket.STREAM_CODEC),
	CHAIN_CONVEYOR_RIDING(Direction.TO_SERVER, ServerboundChainConveyorRidingPacket.STREAM_CODEC),
	CHAIN_PACKAGE_INTERACTION(Direction.TO_SERVER, ChainPackageInteractionPacket.STREAM_CODEC),
	PACKAGE_PORT_CONFIGURATION(Direction.TO_SERVER, PackagePortConfigurationPacket.STREAM_CODEC),
	TRAIN_MAP_REQUEST(Direction.TO_SERVER, TrainMapSyncRequestPacket.STREAM_CODEC),
	CONNECT_FACTORY_PANEL(Direction.TO_SERVER, FactoryPanelConnectionPacket.STREAM_CODEC),
	CONFIGURE_FACTORY_PANEL(Direction.TO_SERVER, FactoryPanelConfigurationPacket.STREAM_CODEC),
	CONFIGURE_REDSTONE_REQUESTER(Direction.TO_SERVER, RedstoneRequesterConfigurationPacket.STREAM_CODEC),
	CONFIGURE_STOCK_KEEPER_CATEGORIES(Direction.TO_SERVER, StockKeeperCategoryEditPacket.STREAM_CODEC),
	REFUND_STOCK_KEEPER_CATEGORY(Direction.TO_SERVER, StockKeeperCategoryRefundPacket.STREAM_CODEC),
	LOCK_STOCK_KEEPER(Direction.TO_SERVER, StockKeeperLockPacket.STREAM_CODEC),
	STOCK_KEEPER_HIDE_CATEGORY(Direction.TO_SERVER, StockKeeperCategoryHidingPacket.STREAM_CODEC),

	// Server to Client
	SYMMETRY_EFFECT(Direction.TO_CLIENT, SymmetryEffectPacket.STREAM_CODEC),
	SERVER_SPEED(Direction.TO_CLIENT, ServerSpeedProvider.Packet.STREAM_CODEC),
	BEAM_EFFECT(Direction.TO_CLIENT, ZapperBeamPacket.STREAM_CODEC),
	CONTRAPTION_STALL(Direction.TO_CLIENT, ContraptionStallPacket.STREAM_CODEC),
	CONTRAPTION_DISASSEMBLE(Direction.TO_CLIENT, ContraptionDisassemblyPacket.STREAM_CODEC),
	CONTRAPTION_BLOCK_CHANGED(Direction.TO_CLIENT, ContraptionBlockChangedPacket.STREAM_CODEC),
	GLUE_EFFECT(Direction.TO_CLIENT, GlueEffectPacket.STREAM_CODEC),
	CONTRAPTION_SEAT_MAPPING(Direction.TO_CLIENT, ContraptionSeatMappingPacket.STREAM_CODEC),
	LIMBSWING_UPDATE(Direction.TO_CLIENT, LimbSwingUpdatePacket.STREAM_CODEC),
	MINECART_CONTROLLER(Direction.TO_CLIENT, MinecartControllerUpdatePacket.STREAM_CODEC),
	FLUID_SPLASH(Direction.TO_CLIENT, FluidSplashPacket.STREAM_CODEC),
	MOUNTED_STORAGE_SYNC(Direction.TO_CLIENT, MountedStorageSyncPacket.STREAM_CODEC),
	GANTRY_UPDATE(Direction.TO_CLIENT, GantryContraptionUpdatePacket.STREAM_CODEC),
	BLOCK_HIGHLIGHT(Direction.TO_CLIENT, HighlightPacket.STREAM_CODEC),
	TUNNEL_FLAP(Direction.TO_CLIENT, TunnelFlapPacket.STREAM_CODEC),
	FUNNEL_FLAP(Direction.TO_CLIENT, FunnelFlapPacket.STREAM_CODEC),
	POTATO_CANNON(Direction.TO_CLIENT, PotatoCannonPacket.STREAM_CODEC),
	SOUL_PULSE(Direction.TO_CLIENT, SoulPulseEffectPacket.STREAM_CODEC),
	PERSISTENT_DATA(Direction.TO_CLIENT, ISyncPersistentData.PersistentDataPacket.STREAM_CODEC),
	SYNC_RAIL_GRAPH(Direction.TO_CLIENT, TrackGraphSyncPacket.STREAM_CODEC),
	SYNC_EDGE_GROUP(Direction.TO_CLIENT, SignalEdgeGroupPacket.STREAM_CODEC),
	ADD_TRAIN(Direction.TO_CLIENT, AddTrainPacket.STREAM_CODEC),
	REMOVE_TRAIN(Direction.TO_CLIENT, RemoveTrainPacket.STREAM_CODEC),
	REMOVE_TE(Direction.TO_CLIENT, RemoveBlockEntityPacket.STREAM_CODEC),
	S_CONFIGURE_TRAIN(Direction.TO_CLIENT, TrainEditReturnPacket.STREAM_CODEC),
	CONTROLS_ABORT(Direction.TO_CLIENT, ControlsStopControllingPacket.STREAM_CODEC),
	S_TRAIN_HUD(Direction.TO_CLIENT, TrainHUDUpdatePacket.Clientbound.STREAM_CODEC),
	S_TRAIN_HONK(Direction.TO_CLIENT, HonkPacket.Clientbound.STREAM_CODEC),
	S_TRAIN_PROMPT(Direction.TO_CLIENT, TrainPromptPacket.STREAM_CODEC),
	CONTRAPTION_RELOCATION(Direction.TO_CLIENT, ContraptionRelocationPacket.STREAM_CODEC),
	TRACK_GRAPH_ROLL_CALL(Direction.TO_CLIENT, TrackGraphRollCallPacket.STREAM_CODEC),
	S_PLACE_ARM(Direction.TO_CLIENT, ArmPlacementPacket.ClientBoundRequest.STREAM_CODEC),
	S_PLACE_EJECTOR(Direction.TO_CLIENT, EjectorPlacementPacket.ClientBoundRequest.STREAM_CODEC),
	S_PLACE_PACKAGE_PORT(Direction.TO_CLIENT, PackagePortPlacementPacket.ClientBoundRequest.STREAM_CODEC),
	UPDATE_ELEVATOR_FLOORS(Direction.TO_CLIENT, ElevatorFloorListPacket.STREAM_CODEC),
	CONTRAPTION_ACTOR_TOGGLE(Direction.TO_CLIENT, ContraptionDisableActorPacket.STREAM_CODEC),
	CONTRAPTION_COLLIDER_LOCK(Direction.TO_CLIENT, ContraptionColliderLockPacket.STREAM_CODEC),
	ATTACHED_COMPUTER(Direction.TO_CLIENT, AttachedComputerPacket.STREAM_CODEC),
	SERVER_DEBUG_INFO(Direction.TO_CLIENT, ServerDebugInfoPacket.STREAM_CODEC),
	PACKAGE_DESTROYED(Direction.TO_CLIENT, PackageDestroyPacket.STREAM_CODEC),
	LOGISTICS_STOCK_RESPONSE(Direction.TO_CLIENT, LogisticalStockResponsePacket.STREAM_CODEC),
	FACTORY_PANEL_EFFECT(Direction.TO_CLIENT, FactoryPanelEffectPacket.STREAM_CODEC),
	PACKAGER_LINK_EFFECT(Direction.TO_CLIENT, WiFiEffectPacket.STREAM_CODEC),
	REDSTONE_REQUESTER_EFFECT(Direction.TO_CLIENT, RedstoneRequesterEffectPacket.STREAM_CODEC),
	KNOCKBACK(Direction.TO_CLIENT, KnockbackPacket.STREAM_CODEC),
	TRAIN_MAP_SYNC(Direction.TO_CLIENT, TrainMapSyncPacket.STREAM_CODEC),
	CLIENTBOUND_CHAIN_CONVEYOR(Direction.TO_CLIENT, ClientboundChainConveyorRidingPacket.STREAM_CODEC),
	SHOP_UPDATE(Direction.TO_CLIENT, ShopUpdatePacket.STREAM_CODEC);;

	private final CustomPacketPayload.Type<? extends CustomPacketPayload> type;
	private final StreamCodec<? super RegistryFriendlyByteBuf, ? extends CustomPacketPayload> codec;
	private final Direction direction;

	<T extends CustomPacketPayload> AllPackets(Direction direction, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		String name = this.name().toLowerCase(Locale.ROOT);
		this.type = new CustomPacketPayload.Type<>(Create.asResource(name));
		this.codec = codec;
		this.direction = direction;
	}

	@SuppressWarnings("unchecked")
	public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
		return (CustomPacketPayload.Type<T>) this.type;
	}

	/**
	 * Catnip's 26.x network API registers a payload by direction rather than by class, and serverbound
	 * payloads that implement {@link SelfHandlingPayload} need no separate handler. Clientbound
	 * handlers cannot be registered here at all - they are side-unsafe - so they live in
	 * {@link com.simibubi.create.foundation.networking.CreateClientPayloadHandlers}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void register(CatnipPayloadRegistrar registrar) {
		String name = this.name().toLowerCase(Locale.ROOT);
		if (direction == Direction.TO_CLIENT) {
			registrar.clientbound(name, (StreamCodec) codec);
			return;
		}
		registrar.selfHandlingServerbound(name, (StreamCodec) codec);
	}

	public static void register() {
		CatnipPayloadRegistrar registrar = new CatnipPayloadRegistrar(Create.ID);
		for (AllPackets packet : AllPackets.values())
			packet.register(registrar);
	}

	private enum Direction {
		TO_SERVER,
		TO_CLIENT
	}
}
