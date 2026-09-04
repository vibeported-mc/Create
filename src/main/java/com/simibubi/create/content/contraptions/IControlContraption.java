package com.simibubi.create.content.contraptions;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.api.lang.Lang;
import net.minecraft.core.BlockPos;

public interface IControlContraption {

	public boolean isAttachedTo(AbstractContraptionEntity contraption);

	public void attach(ControlledContraptionEntity contraption);

	public void onStall();

	public boolean isValid();

	public BlockPos getBlockPosition();

	static enum MovementMode implements INamedIconOptions {

		MOVE_PLACE,
		MOVE_PLACE_RETURNED,
		MOVE_NEVER_PLACE,

		;

		private String translationKey;

		private MovementMode() {
			translationKey = "create.contraptions.movement_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return switch (this) {
			case MOVE_PLACE -> AllIcons.I_MOVE_PLACE;
			case MOVE_PLACE_RETURNED -> AllIcons.I_MOVE_PLACE_RETURNED;
			case MOVE_NEVER_PLACE -> AllIcons.I_MOVE_NEVER_PLACE;
			};
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}

	}

	static enum RotationMode implements INamedIconOptions {

		ROTATE_PLACE,
		ROTATE_PLACE_RETURNED,
		ROTATE_NEVER_PLACE,

		;

		private String translationKey;

		private RotationMode() {
			translationKey = "create.contraptions.movement_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return switch (this) {
			case ROTATE_PLACE -> AllIcons.I_ROTATE_PLACE;
			case ROTATE_PLACE_RETURNED -> AllIcons.I_ROTATE_PLACE_RETURNED;
			case ROTATE_NEVER_PLACE -> AllIcons.I_ROTATE_NEVER_PLACE;
			};
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}

	}

}
