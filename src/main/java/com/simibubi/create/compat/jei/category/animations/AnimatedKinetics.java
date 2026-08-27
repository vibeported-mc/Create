package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import com.simibubi.create.foundation.gui.render.GuiCustomGeometryRenderState;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
		return GuiGameElement.of(partial.get())
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

	/**
	 * Geometry in this widget's scene that is not a block model - a fluid box or a raw buffer.
	 * <p>
	 * A GUI can only reach a submit-node collector through a picture-in-picture pass in 26.2, so the
	 * drawing travels with the render state, and the scene's viewing angle and the element's own
	 * offset are handed to it exactly the way {@link GuiGameElement} hands them to a block element.
	 * The offset is in blocks, on the same axes {@code atLocal} uses; {@code scale} is the widget's
	 * pixels per block, matching what {@code blockElement(...).scale(...)} is given.
	 */
	protected void sceneGeometry(GuiGraphicsExtractor graphics, double scale, double xLocal, double yLocal,
		double zLocal, GuiCustomGeometryRenderState.Geometry geometry) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.scale((float) scale, (float) scale);
		graphics.guiRenderState.addPicturesInPictureState(new GuiCustomGeometryRenderState(geometry,
			new Matrix3x2f(poseStack),
			new GuiElementTransform((float) xLocal, (float) yLocal, (float) zLocal, viewXRot, viewYRot, 0, 0, 0, 0, 0,
				0, 0),
			0, 0, 16, 16, 1, null, null));
		poseStack.popMatrix();
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
