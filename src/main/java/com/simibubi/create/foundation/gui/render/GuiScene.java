package com.simibubi.create.foundation.gui.render;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.element.GuiElementGeometry;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine drawn in a GUI as one picture-in-picture pass, however many parts it is made of.
 *
 * <p>Submitted through {@code GuiGameElement} a part becomes a pass of its own, with a texture and a
 * depth buffer of its own, and the finished textures are then composited as flat quads in the order
 * they were submitted. Parts of one machine cannot resolve against each other that way: the
 * mechanical press's head and basin came out as separate pictures stacked up the screen, and the
 * millstone's body covered its cog outright. Reversing the order only moves the problem, because
 * what is missing is depth, not sequence.
 *
 * <p>Inside {@code parts} one unit is one block and Y points down -- the space 1.21.1's screens and
 * JEI widgets worked in before {@code GuiGameElement} flipped for each element -- so their bodies
 * carry over as they stood, with {@link #part} where they built an element. Offsets they made in
 * screen pixels are {@code scale} units to the block here.
 */
public final class GuiScene {

	private GuiScene() {}

	public static void submit(GuiGraphicsExtractor graphics, double scale, float viewXRot, float viewYRot,
		ILightingSettings lighting, GuiCustomGeometryRenderState.Geometry parts) {
		submit(graphics, scale, viewXRot, viewYRot, lighting, GuiElementTransform.BLOCKS_OF_ROOM, parts);
	}

	/**
	 * @param blocksOfRoom how far from the anchor this scene's parts reach. It sizes the texture, so
	 *                     asking for more than the scene needs costs resolution nothing but memory
	 *                     squared -- only widen it for a scene that is actually being clipped.
	 */
	public static void submit(GuiGraphicsExtractor graphics, double scale, float viewXRot, float viewYRot,
		ILightingSettings lighting, int blocksOfRoom, GuiCustomGeometryRenderState.Geometry parts) {

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		float unit = GuiElementTransform.unitsPerBlock(scale);
		poseStack.scale((float) scale / unit, (float) scale / unit);

		graphics.guiRenderState.addPicturesInPictureState(new GuiCustomGeometryRenderState((ps, collector) -> {
			ps.mulPose(Axis.XP.rotationDegrees(viewXRot));
			ps.mulPose(Axis.YP.rotationDegrees(viewYRot));
			parts.submit(ps, collector);
		}, new Matrix3x2f(poseStack),
			new GuiElementTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, lighting),
			GuiElementTransform.boxMin(scale, blocksOfRoom), GuiElementTransform.boxMin(scale, blocksOfRoom),
			GuiElementTransform.boxMax(scale, blocksOfRoom), GuiElementTransform.boxMax(scale, blocksOfRoom),
			unit, true, null, null));

		poseStack.popMatrix();
	}

	/**
	 * One part, placed as {@code GuiGameElement} placed an element: moved to its offset, flipped, then
	 * turned about the middle of its own block.
	 */
	public static void part(PoseStack poseStack, SubmitNodeCollector collector, BlockStateModel model,
		@Nullable BlockState state, double xLocal, double yLocal, double zLocal, double xRot, double yRot,
		double zRot) {
		poseStack.pushPose();
		poseStack.translate((float) xLocal, (float) yLocal, (float) zLocal);
		GuiElementTransform.flipForGuiRender(poseStack);
		GuiElementGeometry.rotateBlock(poseStack, xRot, yRot, zRot);
		GuiElementGeometry.submitBlockModel(poseStack, collector, model, state, null, 0xFFFFFFFF);
		poseStack.popPose();
	}

	/**
	 * A part turned about the element's origin rather than the middle of its block, as
	 * {@code GuiGameElement.rotate} does against {@code rotateBlock}.
	 */
	public static void partSpun(PoseStack poseStack, SubmitNodeCollector collector, BlockStateModel model,
		@Nullable BlockState state, double xLocal, double yLocal, double zLocal, double xRot, double yRot,
		double zRot) {
		poseStack.pushPose();
		poseStack.translate((float) xLocal, (float) yLocal, (float) zLocal);
		GuiElementTransform.flipForGuiRender(poseStack);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) zRot));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) xRot));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) yRot));
		GuiElementGeometry.submitBlockModel(poseStack, collector, model, state, null, 0xFFFFFFFF);
		poseStack.popPose();
	}

	public static BlockStateModel modelOf(BlockState state) {
		return Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(state);
	}
}
