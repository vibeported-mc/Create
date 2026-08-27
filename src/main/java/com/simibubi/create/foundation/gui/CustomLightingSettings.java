package com.simibubi.create.foundation.gui;

import java.nio.ByteBuffer;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.ILightingSettings;

public class CustomLightingSettings implements ILightingSettings {

	private Vector3f light1;
	private Vector3f light2;
	private @Nullable GpuBuffer buffer;

	protected CustomLightingSettings(float yRot, float xRot) {
		init(yRot, xRot, 0, 0, false);
	}

	protected CustomLightingSettings(float yRot1, float xRot1, float yRot2, float xRot2) {
		init(yRot1, xRot1, yRot2, xRot2, true);
	}

	protected void init(float yRot1, float xRot1, float yRot2, float xRot2, boolean doubleLight) {
		light1 = new Vector3f(0, 0, 1);
		light1.rotate(Axis.YP.rotationDegrees(yRot1));
		light1.rotate(Axis.XN.rotationDegrees(xRot1));

		if (doubleLight) {
			light2 = new Vector3f(0, 0, 1);
			light2.rotate(Axis.YP.rotationDegrees(yRot2));
			light2.rotate(Axis.XN.rotationDegrees(xRot2));
		} else {
			light2 = new Vector3f();
		}
	}

	/**
	 * 26.2 hands the shader its light directions in a uniform buffer rather than as two vectors, so a
	 * custom pair has to live in a buffer of its own. It never changes once built, so it is written the
	 * first time it is needed and kept.
	 */
	@Override
	public void apply() {
		if (buffer == null) {
			buffer = RenderSystem.getDevice()
				.createBuffer(() -> "Create custom lighting UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
					Lighting.UBO_SIZE);

			try (MemoryStack stack = MemoryStack.stackPush()) {
				ByteBuffer contents = Std140Builder.onStack(stack, Lighting.UBO_SIZE)
					.putVec3(light1)
					.putVec3(light2)
					.get();
				RenderSystem.getDevice()
					.createCommandEncoder()
					.writeToBuffer(buffer.slice(), contents);
			}
		}

		RenderSystem.setShaderLights(buffer.slice());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private float yRot1, xRot1;
		private float yRot2, xRot2;
		private boolean doubleLight;

		public Builder firstLightRotation(float yRot, float xRot) {
			yRot1 = yRot;
			xRot1 = xRot;
			return this;
		}

		public Builder secondLightRotation(float yRot, float xRot) {
			yRot2 = yRot;
			xRot2 = xRot;
			doubleLight = true;
			return this;
		}

		public Builder doubleLight() {
			doubleLight = true;
			return this;
		}

		public CustomLightingSettings build() {
			if (doubleLight) {
				return new CustomLightingSettings(yRot1, xRot1, yRot2, xRot2);
			} else {
				return new CustomLightingSettings(yRot1, xRot1);
			}
		}

	}

}
