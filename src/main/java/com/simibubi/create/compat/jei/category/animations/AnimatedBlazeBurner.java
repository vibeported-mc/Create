package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.foundation.render.CachedBufferer;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class AnimatedBlazeBurner extends AnimatedKinetics {

	private HeatLevel heatLevel;

	public AnimatedBlazeBurner withHeat(HeatLevel heatLevel) {
		this.heatLevel = heatLevel;
		return this;
	}

	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-15.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) (xOffset), (float) (yOffset));
		int scale = 23;

		float offset = (Mth.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;

		blockElement(AllBlocks.BLAZE_BURNER.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.submit(graphics);

		PartialModel blaze =
			heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_SUPER : AllPartialModels.BLAZE_ACTIVE;
		PartialModel rods2 = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
			: AllPartialModels.BLAZE_BURNER_RODS_2;

		blockElement(blaze).atLocal(1, 1.8, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.submit(graphics);
		blockElement(rods2).atLocal(1, 1.7 + offset, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.submit(graphics);

		SpriteShiftEntry spriteShift =
			heatLevel == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME : AllSpriteShifts.BURNER_FLAME;

		float spriteWidth = spriteShift.getTarget()
			.getU1()
			- spriteShift.getTarget()
				.getU0();

		float spriteHeight = spriteShift.getTarget()
			.getV1()
			- spriteShift.getTarget()
				.getV0();

		float time = AnimationTickHolder.getRenderTime(Minecraft.getInstance().level);
		float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

		double vScroll = speed * time;
		vScroll = vScroll - Math.floor(vScroll);
		vScroll = vScroll * spriteHeight / 2;

		double uScroll = speed * time / 2;
		uScroll = uScroll - Math.floor(uScroll);
		uScroll = uScroll * spriteWidth / 2;

		// The flame is a partial model rather than a block state, so it goes through a
		// picture-in-picture pass of its own; the flip and the scale to block units that used to be
		// pushed onto the pose stack are part of that pass, leaving the offset to be handed over.
		float uScrollF = (float) uScroll;
		float vScrollF = (float) vScroll;
		sceneGeometry(graphics, scale, 0, -1.8, 0, (poseStack, queue) -> CachedBufferer
			.partial(AllPartialModels.BLAZE_BURNER_FLAME, Blocks.AIR.defaultBlockState())
			.shiftUVScrolling(spriteShift, uScrollF, vScrollF)
			.light(LightCoordsUtil.FULL_BRIGHT)
			.submit(poseStack, RenderTypes.cutoutMovingBlock(), queue));

		matrixStack.popMatrix();
	}

}
