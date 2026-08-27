package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class AnimatedKinetics implements IDrawable {

	public int offset = 0;

	public static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
			.firstLightRotation(12.5f, -45.0f)
			.secondLightRotation(-20.0f, -50.0f)
			.build();

	/**
	 * <b>Only use this method outside of subclasses.</b>
	 * Use {@link #blockElement(BlockState)} if calling from inside a subclass.
	 */
	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(BlockState state) {
		return GuiGameElement.of(state)
				.lighting(DEFAULT_LIGHTING);
	}

	/**
	 * <b>Only use this method outside of subclasses.</b>
	 * Use {@link #blockElement(PartialModel)} if calling from inside a subclass.
	 */
	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(PartialModel partial) {
		return GuiGameElement.of(partial)
				.lighting(DEFAULT_LIGHTING);
	}

	public static float getCurrentAngle() {
		return (AnimationTickHolder.getRenderTime() * 4f) % 360;
	}

	protected BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
	}

	protected PartialModel cogwheel() {
		return AllPartialModels.SHAFTLESS_COGWHEEL;
	}

	private float viewXRot = -15.5f;
	private float viewYRot = 22.5f;

	/**
	 * The orientation this widget's scene is viewed from.
	 * <p>
	 * 26.2's GUI transform stack is two-dimensional, so this is handed to each element rather than
	 * pushed onto the stack once around the whole scene.
	 */
	protected void viewRotation(float xRot, float yRot) {
		viewXRot = xRot;
		viewYRot = yRot;
	}

	protected GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
		return defaultBlockElement(state).viewRotate(viewXRot, viewYRot, 0);
	}

	protected GuiGameElement.GuiRenderBuilder blockElement(PartialModel partial) {
		return defaultBlockElement(partial).viewRotate(viewXRot, viewYRot, 0);
	}

	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 50;
	}

}
