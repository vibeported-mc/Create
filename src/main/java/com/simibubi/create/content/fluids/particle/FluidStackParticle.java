package com.simibubi.create.content.fluids.particle;

import com.simibubi.create.foundation.fluid.FluidAppearance;
import net.minecraft.core.particles.ColorParticleOption;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.fluids.potion.PotionFluid;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidStackParticle extends SingleQuadParticle {
	private final float uo;
	private final float vo;
	private final FluidStack fluid;

	/**
	 * The provider, reached without {@link FluidParticleData} naming a client class.
	 * <p>
	 * The lambda that builds these used to sit in {@code FluidParticleData#getFactory}, and its body
	 * returns a {@link net.minecraft.client.particle.Particle} -- which the JVM resolves when it
	 * verifies the particle data, so a dedicated server could not load it. Behind a static returning
	 * the interface, nothing client-side is named until a client calls it.
	 */
	public static ParticleProvider<FluidParticleData> provider() {
		return (data, world, x, y, z, vx, vy, vz, random) -> create(data.getParticleType(), world, data.getFluid(), x,
			y, z, vx, vy, vz);
	}

	public static FluidStackParticle create(ParticleType<FluidParticleData> type, ClientLevel world, FluidStack fluid,
		double x, double y, double z, double vx, double vy, double vz) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return new BasinFluidParticle(world, fluid, x, y, z, vx, vy, vz);
		return new FluidStackParticle(world, fluid, x, y, z, vx, vy, vz);
	}

	public FluidStackParticle(ClientLevel world, FluidStack fluid, double x, double y, double z, double vx, double vy,
		double vz) {
		super(world, x, y, z, vx, vy, vz, FluidAppearance.stillTexture(fluid));

		this.fluid = fluid;
		this.setSprite(FluidAppearance.stillTexture(fluid));

		this.gravity = 1.0F;
		this.rCol = 0.8F;
		this.gCol = 0.8F;
		this.bCol = 0.8F;
		this.multiplyColor(FluidAppearance.tintColor(fluid));

		this.xd = vx;
		this.yd = vy;
		this.zd = vz;

		this.quadSize /= 2.0F;
		this.uo = this.random.nextFloat() * 3.0F;
		this.vo = this.random.nextFloat() * 3.0F;
	}

	@Override
	protected int getLightCoords(float p_189214_1_) {
		int brightnessForRender = super.getLightCoords(p_189214_1_);
		int skyLight = brightnessForRender >> 20;
		int blockLight = (brightnessForRender >> 4) & 0xf;
		blockLight = Math.max(blockLight, fluid.getFluid()
			.getFluidType()
			.getLightLevel(fluid));
		return (skyLight << 20) | (blockLight << 4);
	}

	protected void multiplyColor(int color) {
		this.rCol *= (float) (color >> 16 & 255) / 255.0F;
		this.gCol *= (float) (color >> 8 & 255) / 255.0F;
		this.bCol *= (float) (color & 255) / 255.0F;
	}

	protected float getU0() {
		return this.sprite.getU((this.uo + 1.0F) / 4.0F);
	}

	protected float getU1() {
		return this.sprite.getU(this.uo / 4.0F);
	}

	protected float getV0() {
		return this.sprite.getV(this.vo / 4.0F);
	}

	protected float getV1() {
		return this.sprite.getV((this.vo + 1.0F) / 4.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (!canEvaporate())
			return;
		if (onGround)
			remove();
		if (!removed)
			return;
		if (!onGround && level.getRandom().nextFloat() < 1 / 8f)
			return;

		Color color = new Color(FluidAppearance.tintColor(fluid));
		level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat()), x, y, z, 0, 0, 0);
	}

	protected boolean canEvaporate() {
		return fluid.getFluid() instanceof PotionFluid;
	}

	/**
	 * Fluid sprites live on the block atlas; the sprite itself decides whether the layer is
	 * translucent.
	 */
	@Override
	protected @NotNull SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.bySprite(sprite);
	}

}
