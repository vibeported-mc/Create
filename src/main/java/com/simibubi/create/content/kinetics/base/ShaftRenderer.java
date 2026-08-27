package com.simibubi.create.content.kinetics.base;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class ShaftRenderer<T extends KineticBlockEntity, S extends KineticBlockEntityRenderer.KineticRenderState>
	extends KineticBlockEntityRenderer<T, S> {

	public ShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(T be) {
		return shaft(getRotationAxisOf(be));
	}

}
