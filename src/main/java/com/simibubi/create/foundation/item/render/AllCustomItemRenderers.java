package com.simibubi.create.foundation.item.render;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItemRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemRenderer;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItemRenderer;
import com.simibubi.create.content.equipment.tool.CardboardSwordItemRenderer;
import com.simibubi.create.content.equipment.wrench.WrenchItemRenderer;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperItemRenderer;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItemRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The items Create draws with a renderer of its own.
 * <p>
 * Each item used to name its renderer from its own class through NeoForge's client item extensions.
 * 26.2 removed that hook, and the renderers are client-only, so they are listed here and registered
 * during client setup.
 */
@OnlyIn(Dist.CLIENT)
public class AllCustomItemRenderers {

	public static void register() {
		CustomRenderedItems.register(AllItems.EXTENDO_GRIP.get(), ExtendoGripItemRenderer::new);
		CustomRenderedItems.register(AllItems.POTATO_CANNON.get(), PotatoCannonItemRenderer::new);
		CustomRenderedItems.register(AllItems.SAND_PAPER.get(), SandPaperItemRenderer::new);
		CustomRenderedItems.register(AllItems.RED_SAND_PAPER.get(), SandPaperItemRenderer::new);
		CustomRenderedItems.register(AllItems.WAND_OF_SYMMETRY.get(), SymmetryWandItemRenderer::new);
		CustomRenderedItems.register(AllItems.CARDBOARD_SWORD.get(), CardboardSwordItemRenderer::new);
		CustomRenderedItems.register(AllItems.WRENCH.get(), WrenchItemRenderer::new);
		CustomRenderedItems.register(AllItems.WORLDSHAPER.get(), WorldshaperItemRenderer::new);
		CustomRenderedItems.register(AllItems.LINKED_CONTROLLER.get(), LinkedControllerItemRenderer::new);
	}

	private AllCustomItemRenderers() {
	}

}
