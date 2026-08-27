package com.simibubi.create.content.logistics.factoryBoard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelState;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelType;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * Draws the panels a factory board is carrying, rotated onto the face the board is mounted on.
 * <p>
 * Everything is handed out unculled: the panels sit proud of the block, so leaving them to the chunk
 * mesher's face culling would drop them against the wall they are mounted to.
 */
public class FactoryPanelModel extends DelegateBlockStateModel {

	public FactoryPanelModel(BlockStateModel originalModel) {
		super(originalModel);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
		List<BlockStateModelPart> parts) {
		FactoryPanelModelData data = new FactoryPanelModelData();
		for (PanelSlot slot : PanelSlot.values()) {
			FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(level, new FactoryPanelPosition(pos, slot));
			if (behaviour == null)
				continue;
			data.states.put(slot, behaviour.count == 0 ? PanelState.PASSIVE : PanelState.ACTIVE);
			data.type = behaviour.panelBE().restocker ? PanelType.PACKAGER : PanelType.NETWORK;
		}
		data.ponder = level instanceof PonderLevel;

		List<BlockStateModelPart> collected = new ArrayList<>();
		super.collectParts(level, pos, state, random, collected);

		List<BakedQuad> quads = new ArrayList<>(BakedModelHelper.quadsOf(collected, null));
		for (PanelSlot panelSlot : PanelSlot.values())
			if (data.states.containsKey(panelSlot))
				addPanel(quads, state, panelSlot, data.type, data.states.get(panelSlot), random, data.ponder);

		parts.add(new PanelPart(quads, collected));
	}

	public void addPanel(List<BakedQuad> quads, BlockState state, PanelSlot slot, PanelType type, PanelState panelState,
		RandomSource rand, boolean ponder) {
		PartialModel factoryPanel = panelState == PanelState.PASSIVE
			? type == PanelType.NETWORK ? AllPartialModels.FACTORY_PANEL : AllPartialModels.FACTORY_PANEL_RESTOCKER
			: type == PanelType.NETWORK ? AllPartialModels.FACTORY_PANEL_WITH_BULB
				: AllPartialModels.FACTORY_PANEL_RESTOCKER_WITH_BULB;

		List<BlockStateModelPart> panelParts = new ArrayList<>();
		factoryPanel.get()
			.collectParts(rand, panelParts);
		List<BakedQuad> quadsToAdd = BakedModelHelper.quadsOf(panelParts, null);

		float xRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state);
		float yRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state);

		for (BakedQuad bakedQuad : quadsToAdd) {
			Vec3 quadNormal = Vec3.atLowerCornerOf(bakedQuad.direction()
				.getUnitVec3i());
			quadNormal = VecHelper.rotate(quadNormal, 180, Axis.Y);
			quadNormal = VecHelper.rotate(quadNormal, xRot + 90, Axis.X);
			quadNormal = VecHelper.rotate(quadNormal, yRot, Axis.Y);

			Vector3f[] positions = BakedQuadHelper.positions(bakedQuad);
			for (int i = 0; i < BakedQuadHelper.VERTEX_COUNT; i++) {
				Vec3 vertex = BakedQuadHelper.getXYZ(positions, i);

				vertex = vertex.add(slot.xOffset * .5, 0, slot.yOffset * .5);
				vertex = VecHelper.rotateCentered(vertex, 180, Axis.Y);
				vertex = VecHelper.rotateCentered(vertex, xRot + 90, Axis.X);
				vertex = VecHelper.rotateCentered(vertex, yRot, Axis.Y);

				BakedQuadHelper.setXYZ(positions, i, vertex);
			}

			Direction newNormal = Direction.getApproximateNearest(quadNormal.x, quadNormal.y, quadNormal.z);
			BakedQuad moved = BakedQuadHelper.withGeometry(bakedQuad, positions, BakedQuadHelper.uvs(bakedQuad));
			quads.add(BakedQuadHelper.withDirection(moved, newNormal));
		}

	}

	/**
	 * The board and its panels, all unculled.
	 */
	private record PanelPart(List<BakedQuad> quads, List<BlockStateModelPart> source) implements BlockStateModelPart {

		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			return direction == null ? quads : List.of();
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean useAmbientOcclusion() {
			return !source.isEmpty() && source.getFirst()
				.useAmbientOcclusion();
		}

		@Override
		public TriState ambientOcclusion() {
			return source.isEmpty() ? TriState.DEFAULT
				: source.getFirst()
					.ambientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return source.getFirst()
				.particleMaterial();
		}

		@Override
		public int materialFlags() {
			int flags = 0;
			for (BakedQuad quad : quads)
				flags |= quad.materialInfo()
					.flags();
			return flags;
		}

	}

	private static class FactoryPanelModelData {
		public PanelType type;
		public EnumMap<PanelSlot, PanelState> states = new EnumMap<>(PanelSlot.class);
		private boolean ponder;
	}

}
