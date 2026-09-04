package com.simibubi.create.content.equipment.clipboard;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/**
 * What a clipboard shows the player it is aimed at: the outline around a block whose settings can be
 * copied, and the tip listing what the buttons would do.
 * <p>
 * Split out of {@link ClipboardValueSettingsHandler}, which is an {@code @EventBusSubscriber} and so
 * is loaded on a dedicated server.
 */
@EventBusSubscriber(Dist.CLIENT)
public class ClipboardValueSettingsHandlerClient {

	@SubscribeEvent
	public static void drawCustomBlockSelection(ExtractBlockOutlineRenderStateEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.hitResult instanceof BlockHitResult target))
			return;

		BlockPos pos = event.getBlockPos();
		BlockState blockstate = event.getBlockState();
		Level level = event.getLevel();

		if (mc.player == null || mc.player.isSpectator())
			return;
		if (!level.getWorldBorder()
			.isWithinBounds(pos))
			return;
		if (!AllBlocks.CLIPBOARD.isIn(mc.player.getMainHandItem()))
			return;
		if (!(level.getBlockEntity(pos) instanceof SmartBlockEntity smartBE))
			return;
		if (!(smartBE instanceof ClipboardBlockEntity) && smartBE.getAllBehaviours()
			.stream()
			.noneMatch(b -> b instanceof ClipboardCloneable cc
				&& cc.writeToClipboard(level.registryAccess(), new CompoundTag(), target.getDirection()))
			&& !(smartBE instanceof ClipboardCloneable))
			return;

		VoxelShape shape = blockstate.getShape(level, pos);
		if (shape.isEmpty())
			return;

		event.addCustomRenderer((renderState, queue, ms, levelRenderState) -> {
			Vec3 camPos = levelRenderState.cameraRenderState.pos;
			ms.pushPose();
			ms.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
			queue.submitCustomGeometry(ms, RenderTypes.lines(),
				(pose, vb) -> TrackBlockOutline.renderShape(shape, pose, vb, true));
			ms.popPose();
			return true;
		});
	}

	public static void clientTick() {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.hitResult instanceof BlockHitResult target))
			return;
		if (!AllBlocks.CLIPBOARD.isIn(mc.player.getMainHandItem()))
			return;
		BlockPos pos = target.getBlockPos();
		if (!(mc.level.getBlockEntity(pos) instanceof SmartBlockEntity smartBE))
			return;

		if (smartBE instanceof ClipboardBlockEntity) {
			List<MutableComponent> tip = new ArrayList<>();
			tip.add(CreateLang.translateDirect("clipboard.actions"));
            tip.add(CreateLang.translateDirect("clipboard.copy_other_clipboard", Component.keybind("key.use")));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
			return;
		}

		ClipboardContent content = mc.player.getMainHandItem()
			.get(AllDataComponents.CLIPBOARD_CONTENT);
		if (content == null)
			return;

		CompoundTag tagElement = content.copiedValues().orElse(null);

		boolean canCopy = smartBE.getAllBehaviours()
			.stream()
			.anyMatch(b -> b instanceof ClipboardCloneable cc
				&& cc.writeToClipboard(mc.level.registryAccess(), new CompoundTag(), target.getDirection()))
			|| smartBE instanceof ClipboardCloneable ccbe
				&& ccbe.writeToClipboard(mc.level.registryAccess(), new CompoundTag(), target.getDirection());

		boolean canPaste = tagElement != null && (smartBE.getAllBehaviours()
			.stream()
			.anyMatch(b -> b instanceof ClipboardCloneable cc && cc.readFromClipboard(mc.level.registryAccess(),
				tagElement.getCompoundOrEmpty(cc.getClipboardKey()), mc.player, target.getDirection(), true))
			|| smartBE instanceof ClipboardCloneable ccbe && ccbe.readFromClipboard(mc.level.registryAccess(),
				tagElement.getCompoundOrEmpty(ccbe.getClipboardKey()), mc.player, target.getDirection(), true));

		if (!canCopy && !canPaste)
			return;

		List<MutableComponent> tip = new ArrayList<>();
		tip.add(CreateLang.translateDirect("clipboard.actions"));
		if (canCopy)
            tip.add(CreateLang.translateDirect("clipboard.to_copy", Component.keybind("key.use")));
		if (canPaste)
            tip.add(CreateLang.translateDirect("clipboard.to_paste", Component.keybind("key.attack")));

		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
	}

	private ClipboardValueSettingsHandlerClient() {}
}
