package com.simibubi.create.content.trains.bogey;

import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.List;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.bogey.BogeySizes.BogeySize;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class BogeyStyle {
	public final Identifier id;
	public final Identifier cycleGroup;
	public final Component displayName;
	public final Supplier<SoundEvent> soundEvent;
	public final ParticleOptions contactParticle;
	public final ParticleOptions smokeParticle;
	public final CompoundTag defaultData;
	private final Map<BogeySizes.BogeySize, Supplier<? extends AbstractBogeyBlock<?>>> sizes;

	private Map<BogeySizes.BogeySize, SizeRenderer> sizeRenderers;

	public BogeyStyle(Identifier id, Identifier cycleGroup, Component displayName,
		Supplier<SoundEvent> soundEvent, ParticleOptions contactParticle, ParticleOptions smokeParticle,
		CompoundTag defaultData, Map<BogeySizes.BogeySize, Supplier<? extends AbstractBogeyBlock<?>>> sizes,
		Map<BogeySizes.BogeySize, Supplier<Supplier<? extends SizeRenderer>>> sizeRenderers) {

		this.id = id;
		this.cycleGroup = cycleGroup;
		this.displayName = displayName;
		this.soundEvent = soundEvent;
		this.contactParticle = contactParticle;
		this.smokeParticle = smokeParticle;
		this.defaultData = defaultData;
		this.sizes = sizes;

		PlatformHelper.INSTANCE.executeOnClientOnly(() -> () -> {
			this.sizeRenderers = new HashMap<>();
			sizeRenderers.forEach((k, v) -> this.sizeRenderers.put(k, v.get()
				.get()));
		});
	}

	public Map<Identifier, BogeyStyle> getCycleGroup() {
		return AllBogeyStyles.getCycleGroup(cycleGroup);
	}

	public Set<BogeySizes.BogeySize> validSizes() {
		return sizes.keySet();
	}

	public AbstractBogeyBlock<?> getBlockForSize(BogeySizes.BogeySize size) {
		return sizes.get(size).get();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AbstractBogeyBlock<?> getNextBlock(BogeySizes.BogeySize currentSize) {
		return Stream.iterate(currentSize.nextBySize(), BogeySizes.BogeySize::nextBySize)
				.filter(sizes::containsKey)
				.findFirst()
				.map(this::getBlockForSize)
				.orElse((AbstractBogeyBlock) getBlockForSize(currentSize));
	}

	/**
	 * Collect this bogey's geometry. 26.2 runs this on the client thread, ahead of submission; see
	 * {@link BogeyRenderer}.
	 */
	public void extract(BogeySize size, float partialTick, int light, float wheelAngle,
		@Nullable CompoundTag bogeyData, boolean inContraption, List<BogeyRenderer.Part> out) {
		if (bogeyData == null)
			bogeyData = new CompoundTag();

		SizeRenderer renderer = sizeRenderers.get(size);
		if (renderer != null)
			renderer.renderer.extract(bogeyData, wheelAngle, partialTick, light, inContraption, out);
	}

	/**
	 * The drop onto the rail is a pose transform rather than part of the geometry, so it belongs
	 * here rather than in extraction.
	 */
	public static void submit(List<BogeyRenderer.Part> parts, PoseStack poseStack, SubmitNodeCollector queue) {
		poseStack.pushPose();
		poseStack.translate(0, -1.5 - 1 / 128f, 0);
		BogeyRenderer.submit(parts, poseStack, queue);
		poseStack.popPose();
	}

	@Nullable
	public BogeyVisual createVisual(BogeySize size, VisualizationContext ctx, float partialTick, boolean inContraption) {
		SizeRenderer renderer = sizeRenderers == null ? null : sizeRenderers.get(size);
		if (renderer != null) {
			return renderer.visualizer.createVisual(ctx, partialTick, inContraption);
		}
		return null;
	}

	public record SizeRenderer(BogeyRenderer renderer, BogeyVisualizer visualizer) {
	}

	public static class Builder {
		protected final Identifier id;
		protected final Identifier cycleGroup;
		protected final Map<BogeySizes.BogeySize, Supplier<? extends AbstractBogeyBlock<?>>> sizes = new HashMap<>();

		protected Component displayName = CreateLang.translateDirect("bogey.style.invalid");
		protected Supplier<SoundEvent> soundEvent = AllSoundEvents.TRAIN2::getMainEvent;
		protected ParticleOptions contactParticle = ParticleTypes.CRIT;
		protected ParticleOptions smokeParticle = ParticleTypes.POOF;
		protected CompoundTag defaultData = new CompoundTag();

		protected final Map<BogeySizes.BogeySize, Supplier<Supplier<? extends SizeRenderer>>> sizeRenderers =
			new HashMap<>();

		public Builder(Identifier id, Identifier cycleGroup) {
			this.id = id;
			this.cycleGroup = cycleGroup;
		}

		public Builder displayName(Component displayName) {
			this.displayName = displayName;
			return this;
		}

		public Builder soundEvent(Supplier<SoundEvent> soundEvent) {
			this.soundEvent = soundEvent;
			return this;
		}

		public Builder contactParticle(ParticleOptions contactParticle) {
			this.contactParticle = contactParticle;
			return this;
		}

		public Builder smokeParticle(ParticleOptions smokeParticle) {
			this.smokeParticle = smokeParticle;
			return this;
		}

		public Builder defaultData(CompoundTag defaultData) {
			this.defaultData = defaultData;
			return this;
		}

		public Builder size(BogeySizes.BogeySize size, Supplier<? extends AbstractBogeyBlock<?>> block,
			 Supplier<Supplier<? extends SizeRenderer>> renderer) {
			this.sizes.put(size, block);
			PlatformHelper.INSTANCE.executeOnClientOnly(() -> () -> {
				this.sizeRenderers.put(size, renderer);
			});
			return this;
		}

		public BogeyStyle build() {
			BogeyStyle entry = new BogeyStyle(id, cycleGroup, displayName, soundEvent, contactParticle, smokeParticle,
				defaultData, sizes, sizeRenderers);
			AllBogeyStyles.BOGEY_STYLES.put(id, entry);
			AllBogeyStyles.CYCLE_GROUPS.computeIfAbsent(cycleGroup, l -> new HashMap<>())
				.put(id, entry);
			return entry;
		}
	}
}
