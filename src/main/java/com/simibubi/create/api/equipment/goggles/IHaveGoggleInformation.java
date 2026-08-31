package com.simibubi.create.api.equipment.goggles;

import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;
// TODO: 1.21.1+ - Move into api package
/**
 * Implement this interface on the {@link BlockEntity} that wants to add info to the goggle overlay
 */
public non-sealed interface IHaveGoggleInformation extends IHaveCustomOverlayIcon {
	/**
	 * This method will be called when looking at a {@link BlockEntity} that implements this interface
	 *
	 * @return {@code true} if the tooltip creation was successful and should be
	 * displayed, or {@code false} if the overlay should not be displayed
	 */
	default boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return false;
	}

	default boolean containedFluidTooltip(List<Component> tooltip, boolean isPlayerSneaking,
										  ResourceHandler<FluidResource> handler) {
		if (handler == null)
			return false;

		if (handler.size() == 0)
			return false;

		LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
		CreateLang.translate("gui.goggles.fluid_container")
			.forGoggles(tooltip);

		boolean isEmpty = true;
		for (int i = 0; i < handler.size(); i++) {
			FluidStack fluidStack = FluidUtil.getStack(handler, i);
			if (fluidStack.isEmpty())
				continue;

			CreateLang.fluidName(fluidStack)
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip, 1);

			CreateLang.builder()
				.add(CreateLang.number(fluidStack.getAmount())
					.add(mb)
					.style(ChatFormatting.GOLD))
				.text(ChatFormatting.GRAY, " / ")
				.add(CreateLang.number(handler.getCapacityAsInt(i, FluidResource.EMPTY))
					.add(mb)
					.style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip, 1);

			isEmpty = false;
		}

		if (handler.size() > 1) {
			if (isEmpty)
				tooltip.remove(tooltip.size() - 1);
			return true;
		}

		if (!isEmpty)
			return true;

		CreateLang.translate("gui.goggles.fluid_container.capacity")
			.add(CreateLang.number(handler.getCapacityAsInt(0, FluidResource.EMPTY))
				.add(mb)
				.style(ChatFormatting.GOLD))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);

		return true;
	}

}
