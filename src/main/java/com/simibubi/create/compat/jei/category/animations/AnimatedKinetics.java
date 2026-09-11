package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import com.simibubi.create.foundation.gui.render.GuiCustomGeometryRenderState;
import com.simibubi.create.foundation.gui.render.GuiScene;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.api.client.gui.element.GuiElementGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.jspecify.annotations.Nullable;
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

	/**
	 * No angle unless a widget asks for one.
	 *
	 * <p>On 1.21.1 {@code blockElement} added no rotation at all -- each widget pushed its own
	 * onto the pose stack, and the ones that wanted none, {@link AnimatedMillstone}, pushed none.
	 * Defaulting to the angle most of them happen to share tilted the millstone off its axis.
	 */
	private float viewXRot = 0;
	private float viewYRot = 0;

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
		float unit = GuiElementTransform.unitsPerBlock(scale);
		poseStack.scale((float) scale / unit, (float) scale / unit);
		graphics.guiRenderState.addPicturesInPictureState(new GuiCustomGeometryRenderState(geometry,
			new Matrix3x2f(poseStack),
			new GuiElementTransform((float) xLocal, (float) yLocal, (float) zLocal, viewXRot, viewYRot, 0, 0, 0, 0, 0,
				0, 0, DEFAULT_LIGHTING),
			GuiElementTransform.boxMin(scale), GuiElementTransform.boxMin(scale), GuiElementTransform.boxMax(scale),
			GuiElementTransform.boxMax(scale), unit, false, null, null));
		poseStack.popMatrix();
	}


	/**
	 * Room for a machine drawn over a depot or basin two blocks below it.
	 *
	 * <p>That lower block's far corner, tilted by the view, lands 2.17 blocks from the anchor -- just
	 * past the default two, so the box cut the bottom off every deployer and spout. Measured by
	 * projecting every part's vertices through the view rotation, across every Create category: these
	 * two were the only widgets that reached past their box.
	 */
	protected static final int TALL_ROOM = 3;

	/**
	 * Draws a whole widget in one picture-in-picture pass.
	 *
	 * <p>Every part a widget is made of has to go into the same pass. Submitted one at a time they
	 * each get a texture and a depth buffer of their own and are then composited as flat quads in
	 * submission order, so they cannot resolve against each other: the mechanical press's head and
	 * its basin came out as separate pictures stacked up the screen, and the millstone's body simply
	 * covered its cog. Sharing a pass gives them back the single depth buffer they had on 1.21.1.
	 *
	 * <p>Inside {@code parts} one unit is one block and Y still points down, which is the space
	 * 1.21.1's widgets worked in before {@link GuiGameElement} flipped for each element -- so those
	 * {@code draw} methods carry over as they were, with {@link #part} for each piece. Offsets those
	 * methods made in screen pixels are {@code scale} units to the block here.
	 */
	protected void scene(GuiGraphicsExtractor graphics, double scale, GuiCustomGeometryRenderState.Geometry parts) {
		scene(graphics, scale, GuiElementTransform.BLOCKS_OF_ROOM, parts);
	}

	protected void scene(GuiGraphicsExtractor graphics, double scale, int blocksOfRoom,
		GuiCustomGeometryRenderState.Geometry parts) {
		GuiScene.submit(graphics, scale, viewXRot, viewYRot, DEFAULT_LIGHTING, blocksOfRoom, parts);
	}

	protected static void part(PoseStack poseStack, SubmitNodeCollector collector, BlockStateModel model,
		@Nullable BlockState state, double xLocal, double yLocal, double zLocal, double xRot, double yRot,
		double zRot) {
		GuiScene.part(poseStack, collector, model, state, xLocal, yLocal, zLocal, xRot, yRot, zRot);
	}

	protected static void partSpun(PoseStack poseStack, SubmitNodeCollector collector, BlockStateModel model,
		@Nullable BlockState state, double xLocal, double yLocal, double zLocal, double xRot, double yRot,
		double zRot) {
		GuiScene.partSpun(poseStack, collector, model, state, xLocal, yLocal, zLocal, xRot, yRot, zRot);
	}

	protected static BlockStateModel modelOf(BlockState state) {
		return GuiScene.modelOf(state);
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
