package com.simibubi.create.content.equipment.blueprint;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.minecraft.tags.TagKey;
import java.util.Optional;
import com.simibubi.create.foundation.recipe.RecipeAccessors;
import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.AttributeFilterWhitelistMode;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute.ItemAttributeEntry;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.crafting.CompoundIngredient;
public class BlueprintItem extends Item {

	public BlueprintItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Direction face = ctx.getClickedFace();
		Player player = ctx.getPlayer();
		ItemStack stack = ctx.getItemInHand();
		BlockPos pos = ctx.getClickedPos()
			.relative(face);

		if (player != null && !player.mayUseItemAt(pos, face, stack))
			return InteractionResult.FAIL;

		Level world = ctx.getLevel();
		HangingEntity hangingentity = new BlueprintEntity(world, pos, face, face.getAxis()
			.isHorizontal() ? Direction.DOWN : ctx.getHorizontalDirection());
		// The tag an item carries for the entity it places is typed in 26.2.
		TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);

		if (entityData != null)
			EntityType.updateCustomEntityTag(world, player, hangingentity, entityData);
		if (!hangingentity.survives())
			return InteractionResult.CONSUME;
		if (!world.isClientSide()) {
			hangingentity.playPlacementSound();
			world.addFreshEntity(hangingentity);
		}

		stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	public static void assignCompleteRecipe(Level level, ItemStacksResourceHandler inv, Recipe<?> recipe) {
		List<Ingredient> ingredients = RecipeAccessors.ingredients(recipe);

		for (int i = 0; i < 9; i++)
			inv.set(i, ItemResource.EMPTY, 0);
		ItemStack stack = RecipeAccessors.result(recipe, level);
		inv.set(9, ItemResource.of(stack), stack.getCount());

		if (recipe instanceof ShapedRecipe shapedRecipe) {
			for (int row = 0; row < shapedRecipe.getHeight(); row++)
				for (int col = 0; col < shapedRecipe.getWidth(); col++) {
					ItemStack filter = convertIngredientToFilter(ingredients.get(row * shapedRecipe.getWidth() + col));
					inv.set(row * 3 + col, ItemResource.of(filter), filter.getCount());
				}
		} else {
			for (int i = 0; i < ingredients.size(); i++) {
				ItemStack filter = convertIngredientToFilter(ingredients.get(i));
				inv.set(i, ItemResource.of(filter), filter.getCount());
			}
		}
	}

	/**
	 * The filter that stands for one of a recipe's ingredients.
	 * <p>
	 * 26.2 flattened ingredients into a single {@link HolderSet} of items: there is no longer a list
	 * of values to walk, each of which was either one item or a tag. A tag-backed ingredient is still
	 * recognisable, because its holder set is unresolved, and everything else is simply its items.
	 */
	private static ItemStack convertIngredientToFilter(Ingredient ingredient) {
		boolean isCompoundIngredient = ingredient.getCustomIngredient() instanceof CompoundIngredient;

		Optional<TagKey<Item>> tag = ingredient.values.unwrapKey();
		if (tag.isPresent())
			return tagFilter(tag.get());

		List<ItemStack> stacks = ingredient.items()
			.map(ItemStack::new)
			.toList();
		if (stacks.isEmpty() || stacks.size() > 18)
			return ItemStack.EMPTY;
		if (stacks.size() == 1)
			return stacks.get(0);

		ItemStack result = AllItems.FILTER.asStack();
		ItemStacksResourceHandler filterItems = AllItems.FILTER.get().getFilterItemHandler(result);
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			filterItems.set(i, ItemResource.of(stack), stack.getCount());
		}
		result.set(AllDataComponents.FILTER_ITEMS, ItemHelper.containerContentsFromHandler(filterItems));
		// A compound ingredient's members were matched exactly, so the filter it becomes does too.
		if (isCompoundIngredient)
			result.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, true);
		return result;
	}

	private static ItemStack tagFilter(TagKey<Item> tag) {
		ItemStack filterItem = AllItems.ATTRIBUTE_FILTER.asStack();
		filterItem.set(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE, AttributeFilterWhitelistMode.WHITELIST_DISJ);
		List<ItemAttributeEntry> attributes = new ArrayList<>();
		ItemAttribute at = new InTagAttribute(tag);
		attributes.add(new ItemAttribute.ItemAttributeEntry(at, false));
		filterItem.set(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES, attributes);
		return filterItem;
	}

}
