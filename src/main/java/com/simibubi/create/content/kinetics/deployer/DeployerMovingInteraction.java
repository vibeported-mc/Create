package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.foundation.utility.NbtValueIO;
import com.simibubi.create.foundation.utility.RegistryNbt;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class DeployerMovingInteraction extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		MutablePair<StructureBlockInfo, MovementContext> actor = contraptionEntity.getContraption()
			.getActorAt(localPos);
		if (actor == null || actor.right == null)
			return false;

		MovementContext ctx = actor.right;
		ItemStack heldStack = player.getItemInHand(activeHand);
		if (heldStack.getItem()
			.equals(AllItems.WRENCH.get())) {
			DeployerBlockEntity.Mode mode = NBTHelper.readEnum(ctx.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
			NBTHelper.writeEnum(ctx.blockEntityData, "Mode",
				mode == DeployerBlockEntity.Mode.PUNCH ? DeployerBlockEntity.Mode.USE : DeployerBlockEntity.Mode.PUNCH);

		} else {
			if (ctx.world.isClientSide())
				return true; // we'll try again on the server side
			DeployerFakePlayer fake = null;

			if (!(ctx.temporaryData instanceof DeployerFakePlayer) && ctx.world instanceof ServerLevel) {
				UUID owner = ctx.blockEntityData.contains("Owner") ? ctx.blockEntityData.read("Owner", UUIDUtil.CODEC).orElse(null) : null;
				DeployerFakePlayer deployerFakePlayer = new DeployerFakePlayer((ServerLevel) ctx.world, owner);
				deployerFakePlayer.onMinecartContraption = ctx.contraption instanceof MountedContraption;
				deployerFakePlayer.getInventory()
					;
			NbtValueIO.loadInventory(fake.getInventory(), ctx.blockEntityData.getListOrEmpty("Inventory"),
				ctx.world.registryAccess());
				ctx.temporaryData = fake = deployerFakePlayer;
				ctx.blockEntityData.remove("Inventory");
			} else
				fake = (DeployerFakePlayer) ctx.temporaryData;

			if (fake == null)
				return false;

			ItemStack deployerItem = fake.getMainHandItem();
			player.setItemInHand(activeHand, deployerItem.copy());
			fake.setItemInHand(InteractionHand.MAIN_HAND, heldStack.copy());
			ctx.blockEntityData.store("HeldItem", ItemStack.OPTIONAL_CODEC,
				RegistryNbt.ops(ctx.world.registryAccess()), heldStack);
			ctx.data.store("HeldItem", ItemStack.OPTIONAL_CODEC, RegistryNbt.ops(ctx.world.registryAccess()),
				heldStack);
		}
//		if (index >= 0)
//			setContraptionActorData(contraptionEntity, index, info, ctx);
		return true;
	}
}
